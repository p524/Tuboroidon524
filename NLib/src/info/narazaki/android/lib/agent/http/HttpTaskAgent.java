package info.narazaki.android.lib.agent.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HttpContext;

import info.narazaki.android.lib.agent.http.task.HttpTaskBase;
import info.narazaki.android.lib.http.ExternalizableCookieStore;
import info.narazaki.android.lib.system.NAndroidSystem;
import android.content.Context;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class HttpTaskAgent implements HttpTaskAgentInterface {
    private static final String TAG = "HttpTaskAgent";
    
    public static interface SaveCookieStoreCallback {
        public void saveCookieStore(final String cookie_bare_data);
    }
    
    private static HashMap<String, SSLSocketFactory> ssl_socket_factory_map_ = new HashMap<String, SSLSocketFactory>();
    
    protected Context context_;
    private final ExecutorService executor_;
    
    private AbstractHttpClient http_client_;
    private BasicCookieStore cookie_store_;
    
    private int timeout_ms_;
    
    protected SaveCookieStoreCallback save_cookie_callback_;
    
    public HttpTaskAgent(Context context, final String user_agent, final HttpHost proxy) {
        super();
        context_ = context;
        executor_ = Executors.newSingleThreadExecutor();
        
        cookie_store_ = createCookieStore();
        http_client_ = createHttpClient(user_agent, proxy);
        
        save_cookie_callback_ = null;
        timeout_ms_ = 10000; // 10秒
    }
    
    @Override
    public Future<?> send(HttpTaskBase task) {
        task.setHttpClient(this, http_client_);
        return executor_.submit(task);
    }
    
    public void setSaveCookieCallback(SaveCookieStoreCallback callback) {
        save_cookie_callback_ = callback;
    }
    
    @Override
    public boolean isOnline() {
        return NAndroidSystem.isOnline(context_);
    }
    
    public int getTimeoutMS() {
        return timeout_ms_; // デフォルト10秒
    }
    
    public void setTimeoutMS(int timeout) {
        timeout_ms_ = timeout;
    }
    
    protected AbstractHttpClient createHttpClient(final String user_agent, final HttpHost proxy) {
        AbstractHttpClient http_client = new DefaultHttpClient();
        http_client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
        http_client.setCookieStore(cookie_store_);
        http_client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, getTimeoutMS());
        http_client.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, getTimeoutMS());
        
        if(proxy != null) {
        	http_client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }
        http_client.addRequestInterceptor(new HttpRequestInterceptor() {
            @Override
            public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
                if (!request.containsHeader("Range") && !request.containsHeader("Accept-Encoding")) {
                    request.addHeader("Accept-Encoding", "gzip");
                }
                if (user_agent != null) {
                    request.removeHeaders("User-Agent");
                    request.addHeader("User-Agent", user_agent);
                }
            }
        });
        http_client.addResponseInterceptor(new HttpResponseInterceptor() {
            @Override
            public void process(final HttpResponse response, final HttpContext context) throws HttpException,
                    IOException {
                HttpEntity entity = response.getEntity();
                if (entity == null) return;
                Header ceheader = entity.getContentEncoding();
                if (ceheader != null) {
                    HeaderElement[] codecs = ceheader.getElements();
                    for (int i = 0; i < codecs.length; i++) {
                        if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                            response.setEntity(new GzipDecompressingEntity(response.getEntity()));
                            return;
                        }
                    }
                }
            }
        });
        
        SSLSocketFactory ssl_socket_factory = this.getSSLSocketFactory();
        if (ssl_socket_factory != null) {
            Scheme https_scheme = new Scheme("https", ssl_socket_factory, 443);
            http_client.getConnectionManager().getSchemeRegistry().register(https_scheme);
        }
        
        return http_client;
    }
    
    private SSLSocketFactory getSSLSocketFactory() {
        try {
            Method method = getClass().getMethod("createSSLSocketFactory", new Class[] {});
            String name = method.getDeclaringClass().getName();
            synchronized (ssl_socket_factory_map_) {
                SSLSocketFactory factory = ssl_socket_factory_map_.get(name);
                if (factory != null) return factory;
                factory = createSSLSocketFactory();
                ssl_socket_factory_map_.put(name, factory);
                return factory;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
    
    public SSLSocketFactory createSSLSocketFactory() {
        try {
            String keystore_filename = System.getProperty("javax.net.ssl.trustStore");
            String keystore_password = System.getProperty("javax.net.ssl.trustStorePassword");
            char[] keystore_password_char = keystore_password != null ? keystore_password.toCharArray() : null;
            
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            if (keystore_filename != null) {
                keystore.load(new FileInputStream(new File(keystore_filename)), keystore_password_char);
            }
            else {
                keystore.load(null, null);
            }
            keystore = setupSSLKeyStore(keystore);
            return new SSLSocketFactory(keystore);
        }
        catch (KeyManagementException e) {
            e.printStackTrace();
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        catch (KeyStoreException e) {
            e.printStackTrace();
        }
        catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        }
        catch (CertificateException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    protected KeyStore setupSSLKeyStore(KeyStore keystore) {
        addGeoTrustGlobalCACertificateEntry(keystore);
        return keystore;
    }
    
    static protected KeyStore addCertificateEntry(KeyStore keystore, String alias, String certificate) {
        ByteArrayInputStream certificate_array = new ByteArrayInputStream(certificate.getBytes());
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(certificate_array);
            keystore.setCertificateEntry(alias, cert);
        }
        catch (KeyStoreException e) {
            e.printStackTrace();
        }
        catch (CertificateException e) {
            e.printStackTrace();
        }
        return keystore;
    }
    
    @Override
    public void setCookieStoreData(String cookie_bare_data) {
        if (cookie_bare_data == null || cookie_bare_data.length() == 0) {
            cookie_store_.clear();
            if (save_cookie_callback_ != null) {
                save_cookie_callback_.saveCookieStore(getCookieStoreData());
            }
            return;
        }
        String cookie_data = URLDecoder.decode(cookie_bare_data);
        try {
            ByteArrayInputStream is = new ByteArrayInputStream(cookie_data.getBytes("ISO8859-1"));
            ObjectInputStream ois = new ObjectInputStream(is);
            cookie_store_ = (ExternalizableCookieStore) ois.readObject();
        }
        catch (Exception e) {
            cookie_store_.clear();
            e.printStackTrace();
        }
        http_client_.setCookieStore(cookie_store_);
    }
    
    @Override
    public void clearCookieStore() {
        cookie_store_.clear();
    }
    
    @Override
    public String getCookieStoreData() {
        ByteArrayOutputStream cookie_out = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(cookie_out);
            oos.reset();
            oos.writeObject(cookie_store_);
            oos.flush();
            return URLEncoder.encode(cookie_out.toString("ISO8859-1"));
        }
        catch (IOException e) {
        }
        return "";
    }
    
    @Override
    public void onHttpTaskFinished(HttpTaskBase task) {
        executor_.submit(new Runnable() {
            @Override
            public void run() {
                if (save_cookie_callback_ != null) {
                    save_cookie_callback_.saveCookieStore(getCookieStoreData());
                }
            }
        });
    }
    
    protected BasicCookieStore createCookieStore() {
        ExternalizableCookieStore cookie_store = new ExternalizableCookieStore();
        return cookie_store;
    }
    
    static class GzipDecompressingEntity extends HttpEntityWrapper {
        public GzipDecompressingEntity(final HttpEntity entity) {
            super(entity);
        }
        
        @Override
        public InputStream getContent() throws IOException, IllegalStateException {
            InputStream wrappedin = wrappedEntity.getContent();
            return new GZIPInputStream(wrappedin);
        }
        
        @Override
        public long getContentLength() {
            return -1;
        }
    }
    
    static private KeyStore addGeoTrustGlobalCACertificateEntry(KeyStore keystore) {
        String geotrust_certificate = "-----BEGIN CERTIFICATE-----\n"
                + "MIIDVDCCAjygAwIBAgIDAjRWMA0GCSqGSIb3DQEBBQUAMEIxCzAJBgNVBAYTAlVT\n"
                + "MRYwFAYDVQQKEw1HZW9UcnVzdCBJbmMuMRswGQYDVQQDExJHZW9UcnVzdCBHbG9i\n"
                + "YWwgQ0EwHhcNMDIwNTIxMDQwMDAwWhcNMjIwNTIxMDQwMDAwWjBCMQswCQYDVQQG\n"
                + "EwJVUzEWMBQGA1UEChMNR2VvVHJ1c3QgSW5jLjEbMBkGA1UEAxMSR2VvVHJ1c3Qg\n"
                + "R2xvYmFsIENBMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2swYYzD9\n"
                + "9BcjGlZ+W988bDjkcbd4kdS8odhM+KhDtgPpTSEHCIjaWC9mOSm9BXiLnTjoBbdq\n"
                + "fnGk5sRgprDvgOSJKA+eJdbtg/OtppHHmMlCGDUUna2YRpIuT8rxh0PBFpVXLVDv\n"
                + "iS2Aelet8u5fa9IAjbkU+BQVNdnARqN7csiRv8lVK83Qlz6cJmTM386DGXHKTubU\n"
                + "1XupGc1V3sjs0l44U+VcT4wt/lAjNvxm5suOpDkZALeVAjmRCw7+OC7RHQWa9k0+\n"
                + "bw8HHa8sHo9gOeL6NlMTOdReJivbPagUvTLrGAMoUgRx5aszPeE4uwc2hGKceeoW\n"
                + "MPRfwCvocWvk+QIDAQABo1MwUTAPBgNVHRMBAf8EBTADAQH/MB0GA1UdDgQWBBTA\n"
                + "ephojYn7qwVkDBF9qn1luMrMTjAfBgNVHSMEGDAWgBTAephojYn7qwVkDBF9qn1l\n"
                + "uMrMTjANBgkqhkiG9w0BAQUFAAOCAQEANeMpauUvXVSOKVCUn5kaFOSPeCpilKIn\n"
                + "Z57QzxpeR+nBsqTP3UEaBU6bS+5Kb1VSsyShNwrrZHYqLizz/Tt1kL/6cdjHPTfS\n"
                + "tQWVYrmm3ok9Nns4d0iXrKYgjy6myQzCsplFAMfOEVEiIuCl6rYVSAlk6l5PdPcF\n"
                + "PseKUgzbFbS9bZvlxrFUaKnjaZC2mqUPuLk/IH2uSrW4nOQdtqvmlKXBx4Ot2/Un\n"
                + "hw4EbNX/3aBd7YdStysVAq45pmp06drE57xNNB6pXE0zX5IJL4hmXXeXxx12E6nV\n"
                + "5fEWCRE11azbJHFwLJhWC9kXtNHjUStedejV0NxPNO3CBWaAocvmMw==\n" + "-----END CERTIFICATE-----";
        String geotrust_alias = "GeoTrust Global CA";
        addCertificateEntry(keystore, geotrust_alias, geotrust_certificate);
        return keystore;
    }
}

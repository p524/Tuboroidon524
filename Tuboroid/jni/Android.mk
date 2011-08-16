LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LDLIBS    := -llog
LOCAL_MODULE    := info_narazaki_android_tuboroid
LOCAL_SRC_FILES := 

########################################
# EntryAnchorFilter
LOCAL_SRC_FILES := $(LOCAL_SRC_FILES) text/span_entryanchorfilter.cpp

# ThreadEntryData
LOCAL_SRC_FILES := $(LOCAL_SRC_FILES) data/threadentrydata.cpp

########################################
include $(BUILD_SHARED_LIBRARY)
########################################


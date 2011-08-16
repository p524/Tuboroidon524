LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_LDLIBS    := -llog
LOCAL_MODULE    := info_narazaki_android_nlib
LOCAL_SRC_FILES := 

########################################
# view.HtmlUtils
LOCAL_SRC_FILES := $(LOCAL_SRC_FILES) view/nsimplelayout.cpp

########################################
# HtmlUtils
LOCAL_SRC_FILES := $(LOCAL_SRC_FILES) text/htmlutils.cpp
# TextUtils
LOCAL_SRC_FILES := $(LOCAL_SRC_FILES) text/textutils.cpp
# WebURLFilter
LOCAL_SRC_FILES := $(LOCAL_SRC_FILES) text/span_weburlfilter.cpp
# LevenshteinDistanceCalc
LOCAL_SRC_FILES := $(LOCAL_SRC_FILES) text/levenshteindistance.cpp

########################################
# ListUtils
LOCAL_SRC_FILES := $(LOCAL_SRC_FILES) list/listutils.cpp


########################################
include $(BUILD_SHARED_LIBRARY)
########################################

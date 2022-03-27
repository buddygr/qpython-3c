LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := qpysdk
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_LDLIBS := \
	-llog \

LOCAL_SRC_FILES := \
	$(LOCAL_PATH)/qpysdk.cpp \

LOCAL_C_INCLUDES += ../

include $(BUILD_SHARED_LIBRARY)

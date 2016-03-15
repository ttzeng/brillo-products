LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := myservice

LOCAL_CFLAGS := -Wall -Werror -Wno-unused-parameter -fexceptions

LOCAL_SRC_FILES := \
	myservice.cpp		\

LOCAL_SHARED_LIBRARIES := \
	libbinderwrapper \
	libbrillo \
	libbrillo-binder \
	libbrillo-stream \
	libchrome \
	libhardware \
	libutils \
	libmraa \

include $(BUILD_EXECUTABLE)

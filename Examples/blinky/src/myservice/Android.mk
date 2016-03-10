LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := myservice

LOCAL_CFLAGS :=
LOCAL_CPPFLAGS:= -fexceptions

LOCAL_SRC_FILES := \
	myservice.cpp		\

LOCAL_SHARED_LIBRARIES := \
	libmraa \
	libc \
	libbase

include $(BUILD_EXECUTABLE)

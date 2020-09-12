LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE    := Max485Serial
LOCAL_SRC_FILES := com_dkss_medical_serial_Max485Serial.c
LOCAL_LDLIBS += -llog
LOCAL_LDLIBS +=-lm
include $(BUILD_SHARED_LIBRARY)

/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_dkss_medical_serial_Max485Serial */

#ifndef _Included_com_dkss_medical_serial_Max485Serial
#define _Included_com_dkss_medical_serial_Max485Serial
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_dkss_medical_serial_Max485Serial
 * Method:    open
 * Signature: (IIICI)I
 */
JNIEXPORT jint JNICALL Java_com_dkss_medical_serial_Max485Serial_open
  (JNIEnv *, jobject, jint, jint, jint, jchar, jint);

/*
 * Class:     com_dkss_medical_serial_Max485Serial
 * Method:    close
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_dkss_medical_serial_Max485Serial_close
  (JNIEnv *, jobject);

/*
 * Class:     com_dkss_medical_serial_Max485Serial
 * Method:    read
 * Signature: ()[I
 */
JNIEXPORT jintArray JNICALL Java_com_dkss_medical_serial_Max485Serial_read
  (JNIEnv *, jobject);

/*
 * Class:     com_dkss_medical_serial_Max485Serial
 * Method:    write
 * Signature: ([IIF)I
 */
JNIEXPORT jint JNICALL Java_com_dkss_medical_serial_Max485Serial_write
  (JNIEnv *, jobject, jintArray, jint, jfloat);

#ifdef __cplusplus
}
#endif
#endif

//
// Created by dbergmann on 07.11.2018.
//
#include <jni.h>
/* Header for class eves_de_pulse_Pulse */

#ifndef PULSE_PULSE_H
#define PULSE_PULSE_H
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _initialize
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_eves_de_pulse_Pulse__1initialize
  (JNIEnv *, jclass);

/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _load
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_eves_de_pulse_Pulse__1load
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _start
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_eves_de_pulse_Pulse__1start
  (JNIEnv *, jclass, jlong, jint, jint);

/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _onFrame
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_eves_de_pulse_Pulse__1onFrame
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _facesCount
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_eves_de_pulse_Pulse__1facesCount
  (JNIEnv *, jclass, jlong);

/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _face
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_eves_de_pulse_Pulse__1face
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _relativeMinFaceSize
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_eves_de_pulse_Pulse__1relativeMinFaceSize
  (JNIEnv *, jclass, jlong);

/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _maxSignalSize
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_eves_de_pulse_Pulse__1maxSignalSize
  (JNIEnv *, jclass, jlong);

/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _faceDetection
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_eves_de_pulse_Pulse__1faceDetection__J
  (JNIEnv *, jclass, jlong);

/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _faceDetection
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_eves_de_pulse_Pulse__1faceDetection__JZ
  (JNIEnv *, jclass, jlong, jboolean);

/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _magnification
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_eves_de_pulse_Pulse__1magnification__J
  (JNIEnv *, jclass, jlong);

/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _magnification
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_eves_de_pulse_Pulse__1magnification__JZ
  (JNIEnv *, jclass, jlong, jboolean);

/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _magnificationFactor
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_eves_de_pulse_Pulse__1magnificationFactor__J
  (JNIEnv *, jclass, jlong);

/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _magnificationFactor
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL Java_eves_de_pulse_Pulse__1magnificationFactor__JI
  (JNIEnv *, jclass, jlong, jint);

/*
 * Class:     eves_de_pulse_Pulse
 * Method:    _destroy
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_eves_de_pulse_Pulse__1destroy
  (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif

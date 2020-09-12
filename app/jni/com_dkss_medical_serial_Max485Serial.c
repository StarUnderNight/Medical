#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <termios.h>
#include <errno.h>
#include <string.h>

#include <stdint.h>
#include <android/log.h>
#include <sys/ioctl.h>
#include <jni.h>

#undef  TCSAFLUSH
#define TCSAFLUSH  TCSETSF
#ifndef _TERMIOS_H_
#define _TERMIOS_H_
#endif
#ifndef _Included_com_dkss_medical_serial_Max485Serial
#define _Included_com_dkss_medical_serial_Max485Serial
#ifdef __cplusplus
extern "C" {
#endif

int fd=0;
int fd_ctl = 0;


//open max485_ctl device
int open_ctl(){
    if(fd_ctl<=0){
        fd_ctl = open("/dev/max485_ctl_pin", O_RDWR|O_NDELAY|O_NOCTTY);
    }
    return fd_ctl;
}

//close max485_ctl
int close_ctl(){
    if(fd_ctl>0){
        close(fd_ctl);
    }
}

int ioctl_ctl(int num,int en){
    int ret = ioctl(fd_ctl,en,num);
    if(ret<0){
        __android_log_print(ANDROID_LOG_INFO, "serial", "ioctl option fail");
        return -1;
    }
    return ret;
}

int read_data(int fd,unsigned char *buffer,int buffer_size){
    int fs_sel;
    int ret = -1;
    fd_set fs_read;
    struct timeval time;

    FD_ZERO(&fs_read);
    FD_SET(fd, &fs_read);

    time.tv_sec = 1;
    time.tv_usec = 0;

    fs_sel = select(fd + 1, &fs_read, NULL, NULL, &time);
    if (fs_sel){
         ret = read(fd, buffer, buffer_size);
         if (ret == 0){
                return 0;
         }
         if (ret < 0){
                return -1;
         }
         return ret;
    }else{
        return -2;
    }
}


int set_opt(int fd,int nSpeed, int nBits, char nEvent, int nStop)
{
	struct termios options;
	if  ( tcgetattr( fd,&options)  !=  0) {
		__android_log_print(ANDROID_LOG_INFO, "serial", "tcgetattr  get fail");
		return -1;
	}

	options.c_cflag  |=  CLOCAL | CREAD;
	options.c_cflag &= ~CSIZE;

	switch( nBits ){
	    case 7:
		    options.c_cflag |= CS7;
		break;
	    case 8:
		    options.c_cflag |= CS8;
		break;
	}

	switch( nEvent ){
	    case 'O':
	    case 'o':
		    options.c_cflag |= PARENB;
		    options.c_cflag |= PARODD;
		    options.c_iflag |= (INPCK | ISTRIP);
		break;
	    case 'E':
	    case 'e':
		    options.c_iflag |= (INPCK | ISTRIP);
		    options.c_cflag |= PARENB;
		    options.c_cflag &= ~PARODD;
		break;
	    case 'N':
	    case 'n':
		    options.c_cflag &= ~PARENB;
		    options.c_iflag &= ~INPCK;
		break;
	}


	switch( nSpeed ){
	    case 2400:
		    cfsetispeed(&options, B2400);
		    cfsetospeed(&options, B2400);
		break;
    	case 4800:
	    	cfsetispeed(&options, B4800);
		    cfsetospeed(&options, B4800);
		break;
	    case 9600:
		    cfsetispeed(&options, B9600);
		    cfsetospeed(&options, B9600);
		break;
	    case 38400:
		    cfsetispeed(&options, B38400);
            cfsetospeed(&options, B38400);
		break;
	    case 115200:
		    cfsetispeed(&options, B115200);
		    cfsetospeed(&options, B115200);
		break;
	    case 460800:
		    cfsetispeed(&options, B460800);
		    cfsetospeed(&options, B460800);
		break;
	    default:
		    cfsetispeed(&options, B9600);
		    cfsetospeed(&options, B9600);
		break;
	}

    //停止位
	if( nStop == 1 )
		options.c_cflag &=  ~CSTOPB;
	else if ( nStop == 2 )
		options.c_cflag |=  CSTOPB;

	//添加未有的配置
	options.c_iflag &= ~(IXON | IXOFF | IXANY);  //不要软件流控
    options.c_iflag &= ~(INLCR | ICRNL);  //不要回车和换行

     //修改输出模式，原始数据输出
    options.c_oflag &= ~OPOST;

    options.c_lflag &= ~(ICANON | ECHO | ECHOE | ISIG);//原始模式

	options.c_cc[VTIME]  = 1;
	options.c_cc[VMIN] = 1;

	tcflush(fd,TCIFLUSH);
	if((tcsetattr(fd,TCSANOW,&options))!=0){
		__android_log_print(ANDROID_LOG_INFO, "serial", "tcgsetattr  set fail");
		return -1;
	}
	return 0;
}

JNIEXPORT jint JNICALL Java_com_dkss_medical_serial_Max485Serial_open
  (JNIEnv *env, jobject obj, jint Port, jint Rate, jint nBits, jchar nEvent, jint nStop)
{
  if(fd <= 0){
		if(0 == Port){
			__android_log_print(ANDROID_LOG_INFO, "serial", "open fd /dev/ttySAC0");
			fd=open("/dev/ttySAC0",O_RDWR|O_NDELAY|O_NOCTTY);
		}else if(1 == Port){
			__android_log_print(ANDROID_LOG_INFO, "serial", "open fd /dev/ttySAC1");
			fd=open("/dev/ttySAC1",O_RDWR|O_NDELAY|O_NOCTTY);
		}else if(2 == Port){
			__android_log_print(ANDROID_LOG_INFO, "serial", "open fd /dev/ttySAC2");
			fd=open("/dev/ttySAC2",O_RDWR|O_NDELAY|O_NOCTTY);
		}else if(3 == Port){
			__android_log_print(ANDROID_LOG_INFO, "serial", "open fd /dev/ttySAC3");
			fd=open("/dev/ttySAC3",O_RDWR|O_NDELAY|O_NOCTTY);
		}else if(4 == Port){
			__android_log_print(ANDROID_LOG_INFO, "serial", "open fd /dev/ttyUSB0");
			fd=open("/dev/ttyUSB0",O_RDWR|O_NDELAY|O_NOCTTY);
		}else if(5 == Port){
			__android_log_print(ANDROID_LOG_INFO, "serial", "open fd /dev/ttyUSB1");
			fd=open("/dev/ttyUSB1",O_RDWR|O_NDELAY|O_NOCTTY);
		}else{
			__android_log_print(ANDROID_LOG_INFO, "serial", "Parameter Error serial not found");
			fd = 0;
			return -1;
		}

		if(fcntl(fd,F_SETFL,0)<0){
		    __android_log_print(ANDROID_LOG_INFO, "serial", "fcntl set fail");
		    return -1;
		}
		
		set_opt(fd, Rate, nBits, nEvent, nStop);

  }

  jint ret_ctl = open_ctl();
  __android_log_print(ANDROID_LOG_INFO, "serial", "serial open ret_ctl=%d,fd_ctl = %d,fd=%d",ret_ctl,fd_ctl,fd);

   return fd;
}


JNIEXPORT jint JNICALL Java_com_dkss_medical_serial_Max485Serial_close
  (JNIEnv *env, jobject obj)
  {
	if(fd > 0){
	    close(fd);
	}
	close_ctl();
	return 1;
  }


JNIEXPORT jintArray JNICALL Java_com_dkss_medical_serial_Max485Serial_read
  (JNIEnv *env, jobject obj)
 {
        ioctl_ctl(0,0);   //set 485 transport direction as read
		unsigned char buffer[512];
		int BufToJava[512];
		int len = 0, i = 0;
		
		memset(buffer, 0, sizeof(buffer));
		memset(BufToJava, 0, sizeof(BufToJava));
		
		len =  read_data(fd, buffer, 512);

		if(len <= 0){
		    __android_log_print(ANDROID_LOG_INFO, "serial", "read data len = -1");
		    return NULL;
		}

        for(i = 0;i < len;i++){
             BufToJava[i] = buffer[i];
        }

		jintArray array = (*env)-> NewIntArray(env, len);
		(*env)->SetIntArrayRegion(env, array, 0, len, BufToJava);

		return array;	
  }

JNIEXPORT jint JNICALL Java_com_dkss_medical_serial_Max485Serial_write
  (JNIEnv *env, jobject obj, jintArray buf, jint buflen, jfloat sleepTime)
 {
    ioctl_ctl(0,1);
	jsize len = buflen;
	
	if(len <= 0) {
        return -1;
	}

	jintArray array = (*env)-> NewIntArray(env, len);  

	if(array == NULL){array=NULL;return -1;}

	jint *body = (*env)->GetIntArrayElements(env, buf, 0);

	jint i = 0;
	unsigned char num[len];

	for (i=0; i <len; i++) {
	    num[i] = body[i];
	}

	jint w_len = 0;
	if((w_len = write(fd, num, len)) != len){
	    array = NULL;
	    __android_log_print(ANDROID_LOG_INFO, "serial", "write fail, w_len = %d,len = %d",w_len,len);
	    return w_len;
	}
	//休眠
	//usleep(10*1000);电量仪
	usleep(sleepTime*1000);

    //释放
    (*env)->ReleaseByteArrayElements(env,buf,body,JNI_ABORT);
	array = NULL;

	return -1;
  }
  
#ifdef __cplusplus
}
#endif
#endif

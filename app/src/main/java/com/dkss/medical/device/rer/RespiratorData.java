package com.dkss.medical.device.rer;

import com.dkss.medical.device.rer.Respirator;
import com.dkss.medical.util.DkssUtil;

import java.io.*;
import java.util.*;


public class RespiratorData {
    protected static HashMap<String,byte[]> mConfig = null;
    protected static final String CMD_Ah = "<02Ahff0A>"; //APP请求主板机器ID的指令(Ah)
    protected static final String CMD_Ai = "<02Aiff09>"; //APP请求通气控制参数的指令(Ai)
    protected static final String CMD_Aj = "<02Ajff08>"; //APP请求报警阈值的指令 (Aj)
    protected static final String CMD_An = "<02Anff04>"; //心跳包

    /*****
     * 机器返回APP数据
     *
     * *********/
    //主板发送机器ID(Mh)
    static ArrayList<byte[]> data_Mh(byte[] btArr){
        String str = new String(btArr);//原始字符串
        String pkgLenTemp = str.substring(1,3);
        String pkgLen = Integer.valueOf(pkgLenTemp,16).toString();//长度
        String pkg_checkSum =  str.substring(Integer.valueOf(pkgLen) + 5,Integer.valueOf(pkgLen) + 7);//校验和
        if (!(pkg_checkSum.equals( parseCheckSum(btArr)))){//检验校验和是否正确
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder("");//解析后的字符串
        String pkg_order = str.substring(3,4);//命令
        String pkg_paramType = str.substring(4,5);//参数类型
        if (!pkg_order.equals("M")||!pkg_paramType.equals("h")){
            return null;
        }
        String pkg_param = str.substring(5,Integer.valueOf(pkgLen) + 5);//参数1 机器ID
        ArrayList<byte[]> paramList = new ArrayList<>();
        paramList.add(DkssUtil.constructPayload( (Respirator.dataIDMap.get("pMh1")) ,pkg_param));
        return paramList;
    }

    //构建数据包 Mi
    //6.2.2	主板发送通气控制参数(Mi)
    static ArrayList<byte[]> data_Mi(byte[] btArr) throws UnsupportedEncodingException {
        String str = new String(btArr);//原始字符串
        String pkgLenTemp = str.substring(1,3);
        String pkgLen = Integer.valueOf(pkgLenTemp,16).toString();//长度
        String pkg_checkSum =  str.substring(Integer.valueOf(pkgLen) + 5,Integer.valueOf(pkgLen) + 7);//校验和
        if (!(pkg_checkSum.equals( parseCheckSum(btArr)))){//检验校验和是否正确
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder("");//解析后的字符串
        String pkg_order = str.substring(3,4);//命令
        String pkg_paramType = str.substring(4,5);//参数类型
        if (!pkg_order.equals("M")||!pkg_paramType.equals("i")){
            return null;
        }
        String param[] = new String[25];
        ArrayList<byte[]> paramList = new ArrayList<>();
        //修改参数对应的密钥
        param[0] = hexStr2decStr(exchangeOrder(str.substring(5,9)));
        paramList.add(DkssUtil.constructPayload( (Respirator.dataIDMap.get("pMi1")) ,hexStr2UnsignedShort(param[0])));
        //用户类型
        param[1] = str.substring(9,11);
        System.out.println("PMi2"+Integer.toBinaryString((int)hexStr2char(param[1])));


        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi2")),hexStr2char(param[1])));
        //呼吸模式
        param[2] = str.substring(11,13);
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi3")),hexStr2char(param[2])));
        //氧气浓度
        param[3] = str.substring(13,15);
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi4")),param[3]));
        //潮气量(Vt)
        param[4] = exchangeOrder(str.substring(15,19));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi5")),hexStr2UnsignedShort(param[4])));
        //吸气时间
        param[5] = exchangeOrder(str.substring(19,23));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi6")),hexStr2UnsignedShort(param[5])));
        //呼气时间
        param[6] = exchangeOrder(str.substring(23,27));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi7")),hexStr2UnsignedShort(param[6])));
        //压力上升时间
        param[7] = exchangeOrder(str.substring(27,31));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi8")),param[7]));
        //呼吸频率(Freq)
        param[8] = str.substring(31,33);
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi9")),hexStr2char(param[8])));
        //叹息周期SIGH_CIRCLE
        param[9] = str.substring(33,35);
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi10")),hexStr2char(param[9])));
        //吸气流速Finsp
        param[10] = exchangeOrder(str.substring(35,39));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi11")),hexStr2UnsignedShort(param[10])));
        //触发类型
        param[11] = str.substring(39,41);
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi12")),hexStr2char(param[11])));
        //触发阈值,有符号
        param[12] = str.substring(41,43);
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi13")),hexStr2char(param[12])));
        //平台时间
        param[13] = str.substring(43,45);
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi14")),hexStr2char(param[13])));
        //呼气触发
        param[14] = str.substring(45,47);
        // paramList.add( DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi5")),param[14]));
        //呼气末正压/持续正压CPAP
        param[15] = str.substring(47,49);
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi16")),hexStr2char(param[15])));
        //吸气压力Pinsp
        param[16] = str.substring(49,51);
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi17")),hexStr2char(param[16])));
        //支持压力
        param[17] = str.substring(51,53);
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi18")),param[17]));
        //触发窗TRIGGER_WINDOW
        param[18] = str.substring(53,55);
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi19")),hexStr2char(param[18])));
        //窒息压力D
        param[19] = str.substring(55,57);
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi20")),param[19]));
        //窒息频率
        param[20] = str.substring(57,59);
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi21")),param[20]));
        //压力限制
        param[21] = str.substring(59,61);
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi22")),param[21]));
        //吸呼转换压力ITESP
        param[22] = str.substring(61,63);
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi23")),hexStr2char(param[22])));
        //CO2模块设置
        param[23] = exchangeOrder(str.substring(63,67));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMi24")),(char) hexStr2UnsignedShort(param[23])));
        return paramList;
    }
    //6.2.3	主板发送报警阈值(Mj)
    static ArrayList<byte[]> data_Mj(byte[] btArr) throws UnsupportedEncodingException {
        String str = new String(btArr);//原始字符串
        String pkgLenTemp = str.substring(1,3);
        String pkgLen = Integer.valueOf(pkgLenTemp,16).toString();//长度
        String pkg_checkSum =  str.substring(Integer.valueOf(pkgLen) + 5,Integer.valueOf(pkgLen) + 7);//校验和
        if (!(pkg_checkSum.equals( parseCheckSum(btArr)))){//检验校验和是否正确
            // System.out.println("checksum failed");
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder("");//解析后的字符串
        String pkg_order = str.substring(3,4);//命令
        String pkg_paramType = str.substring(4,5);//参数类型
        if (!pkg_order.equals("M")||!pkg_paramType.equals("j")){
            // System.out.println("not Mj");
            return null;
        }

        String param[] = new String[14];
        ArrayList<byte[]>  paramList = new ArrayList<>();
        //修改参数对应的密钥
//        param[0] = exchangeOrder(hexStr2decStr(exchangeOrder(str.substring(5,9))));
//        paramList.add( DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMj1")) ,hexStr2UnsignedShort(param[0])));
        //气道压力上限Pmax
        param[1] = str.substring(9,11);
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMj2")) ,hexStr2char(param[1])));
        //气道压力下限
        param[2] = str.substring(11,13);
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMj3")) ,param[2]));
        //分钟通气量上限
        param[3] = str.substring(13,15);
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMj4")) ,hexStr2char(param[3])));
        //分钟通气量下限
        param[4] = str.substring(15,17);
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMj5")) ,hexStr2char(param[4])));
        //呼吸频率上限
        param[5] = str.substring(17,19);
        //paramList.add( DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMj6")) ,param[5]));
        //窘息时间
        param[6] = str.substring(19,21);
        paramList.add( DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMj7")) ,hexStr2char(param[6])));
        //氧浓度上限
        param[7] = str.substring(21,23);
        //paramList.add( DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMj8")) ,param[7]));
        //氧浓度下限
        param[8] = str.substring(23,25);
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMj9")) ,param[8]));
        //潮气量上限
        param[9] = exchangeOrder(str.substring(25,29));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMj10")) ,param[9]));
        //潮气量下限
        param[10] = exchangeOrder(str.substring(29,33));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMj11")) ,param[10]));
        //呼末二氧化碳上限
        param[11] = str.substring(33,35);
        //System.out.println("Co2 上限" + param[11] + bytes2hex(param[11].getBytes()));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMj12")) ,hexStr2char(param[11])));

        //呼末二氧化碳下限
        param[12] = str.substring(35,37);
        //System.out.println("Co2 下限" + param[12] + bytes2hex(param[12].getBytes()));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMj13")) ,hexStr2char(param[12])));
        return paramList;
    }
    //6.2.4	呼气末通气参数监测(Mk)
    static ArrayList<byte[]> data_Mk(byte[] btArr) throws UnsupportedEncodingException {
        String str = new String(btArr);//原始字符串
        String pkgLenTemp = str.substring(1,3);
        String pkgLen = Integer.valueOf(pkgLenTemp,16).toString();//长度
        String pkg_checkSum =  str.substring(Integer.valueOf(pkgLen) + 5,Integer.valueOf(pkgLen) + 7);//校验和
        if (!(pkg_checkSum.equals( parseCheckSum(btArr)))){//检验校验和是否正确
            // System.out.println("checksum failed");
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder("");//解析后的字符串
        String pkg_order = str.substring(3,4);//命令
        String pkg_paramType = str.substring(4,5);//参数类型
        if (!pkg_order.equals("M")||!pkg_paramType.equals("k")){
            //  System.out.println("not Mk");
            return null;
        }

        String param[] = new String[26];
        ArrayList<byte[]>  paramList = new ArrayList<>();
        //气道峰压(Ppeak)
        param[0] = exchangeOrder(str.substring(5,9));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk1")) ,hexStr2SignedShort(param[0])));
        //平台压
        param[1] = exchangeOrder(str.substring(9,13));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk2")) ,param[1]));
        //平均压(Pmean)
        param[2] = exchangeOrder(str.substring(13,17));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk3")) ,hexStr2SignedShort(param[2])));
        //呼气末正压
        param[3] = exchangeOrder(str.substring(17,21));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk4")) ,param[3]));
        //P0.1
        param[4] = exchangeOrder(str.substring(21,25));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk5")) ,param[4]));
        //最大吸气流速
        param[5] = exchangeOrder(str.substring(25,29));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk6")) ,param[5]));
        //最大呼气流速
        param[6] = exchangeOrder(str.substring(29,33));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk7")) ,param[6]));
        //吸入潮气量(Vti)
        param[7] = exchangeOrder(str.substring(33,37));

        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk8")) ,hexStr2SignedShort(param[7])));
        //呼出潮气量
        param[8] = exchangeOrder(str.substring(37,41));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk9")) ,param[8]));
        //吸入分钟通气量
        param[9] = exchangeOrder(str.substring(41,45));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk10")) ,hexStr2SignedShort(param[9])));
        //呼出分钟通气量
        param[10] = exchangeOrder(str.substring(45,49));
        // paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk11")) ,param[10]));
        //第一秒呼气量 TVE_1S
        param[11] = exchangeOrder(str.substring(49,53));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk12")) ,param[11]));
        //吸气时间
        param[12] = exchangeOrder(str.substring(53,57));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk13")) ,param[12]));
        //呼气时间
        param[13] = exchangeOrder(str.substring(57,61));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk14")) ,param[13]));
        //呼吸时间
        param[14] = exchangeOrder(str.substring(61,65));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk15")) ,param[14]));
        //吸呼比
        param[15] = exchangeOrder(str.substring(65,69));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk16")) ,param[15]));
        //机械通气频率(Fsp)
        param[16] = exchangeOrder(str.substring(69,73));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk17")) ,hexStr2SignedShort(param[16])));
        //自主呼吸频率
        param[17] = exchangeOrder(str.substring(73,77));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk18")) ,param[17]));
        //气阻
        param[18] = exchangeOrder(str.substring(77,81));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk19")) ,param[18]));
        //静态顺应性
        param[19] = exchangeOrder(str.substring(81,85));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk20")) ,param[19]));
        //动态顺应性
        param[20] = exchangeOrder(str.substring(85,89));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk21")) ,param[20]));
        //吸入氧浓度
        param[21] = exchangeOrder(str.substring(89,93));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk22")) ,hexStr2SignedShort(param[21])));
        //呼吸功
        param[22] = exchangeOrder(str.substring(93,97));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk23")) ,param[22]));
        //浅快呼吸指数
        param[23] = exchangeOrder(str.substring(97,101));
        //paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk24")) ,param[23]));
        //ETCO2
        param[24] = exchangeOrder(str.substring(101,105));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMk25")) ,hexStr2SignedShort(param[24])));
        return paramList;
    }
    //6.2.5	波形参数(Mn)
    static ArrayList<byte[]> data_Mn(byte[] btArr) throws UnsupportedEncodingException {
        String str = new String(btArr);//原始字符串
        String pkgLenTemp = str.substring(1,3);
        String pkgLen = Integer.valueOf(pkgLenTemp,16).toString();//长度
        String pkg_checkSum =  str.substring(Integer.valueOf(pkgLen) + 5,Integer.valueOf(pkgLen) + 7);//校验和
        if (!(pkg_checkSum.equals( parseCheckSum(btArr)))){//检验校验和是否正确
            //System.out.println("checksum failed");
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder("");//解析后的字符串
        String pkg_order = str.substring(3,4);//命令
        String pkg_paramType = str.substring(4,5);//参数类型
        if (!pkg_order.equals("M")||!pkg_paramType.equals("n")){
            // System.out.println("not Mn");
            return null;
        }

        String param[] = new String[6];
        ArrayList<byte[]> paramList = new ArrayList<>();
        //通气状态
        short  param0_temp = hexStr2UnsignedShort(exchangeOrder(str.substring(5,9)));
        param[0] = exchangeOrder(str.substring(5,9));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMn1")) ,hexStr2UnsignedShort(param[0])));

        //气道压力
        param[1] = exchangeOrder(str.substring(9,13));
        paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMn2")) ,hexStr2SignedShort(param[1])));
        //呼吸流速
        param[2] = exchangeOrder(str.substring(13,17));
        // paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMn3")) ,param[2]));
        //呼吸容量
        param[3] = str.substring(17,21);
        // paramList.add( DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMn4")) ,param[3]));
        //Co2 波形值
        param[4] = str.substring(21,25);
        paramList.add(  DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMn5")) ,hexStr2UnsignedShort(param[4])));

        return paramList;
    }
    //6.2.6	报警监测(Mo)
    static ArrayList<byte[]> data_Mo(byte[] btArr) throws UnsupportedEncodingException {
        String str = new String(btArr);//原始字符串
        String pkgLenTemp = str.substring(1,3);
        String pkgLen = Integer.valueOf(pkgLenTemp,16).toString();//长度
        String pkg_checkSum =  str.substring(Integer.valueOf(pkgLen) + 5,Integer.valueOf(pkgLen) + 7);//校验和
        if (!(pkg_checkSum.equals( parseCheckSum(btArr)))){//检验校验和是否正确
            // System.out.println("checksum failed");
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder("");//解析后的字符串
        String pkg_order = str.substring(3,4);//命令
        String pkg_paramType = str.substring(4,5);//参数类型
        if (!pkg_order.equals("M")||!pkg_paramType.equals("o")){
            //  System.out.println("not Mo");
            return null;
        }


        short param1 = Short.parseShort(exchangeOrder(str.substring(5,9)));

        short p = 1;
        int i = 0;
        int[] param = new int[19];
        for(i=0;i<16;i++){
            if( (param1&p) == p ){
                param[i] = 16 + i;
            }
            p = (short)(p<<1);
        }

        short param2 = Short.parseShort(exchangeOrder(str.substring(9,13)));
        p = 1;
        for(;i<19;i++){
            if( (param1&p) == p ){
                param[i] = 16 + i;
            }
            p = (short)(p<<1);
        }

        //机器ID
        //param[19] = new String(btArr).substring(17,41);
        ArrayList<byte[]> paramList = new ArrayList<>();
        for (i=0; i<param.length;i++){
            if (param[i]!=0){
                paramList.add(DkssUtil.constructPayload(  (Respirator.dataIDMap.get("pMo" +(i+1) )),param[i]));
            }

        }
        return  paramList;

    }


    /**字符转字节
     *
     * @param str
     * @return byte[]
     * @throws UnsupportedEncodingException
     */
    static byte[] getBytes(String str) throws UnsupportedEncodingException {
        return str.getBytes("ISO-8859-1");
    }
    //获得校验和的16进制数由字符表示， 0x08 表示为字符 ’08‘，传入计算后的校验和，返回对应的两个字符的字节数组

    /***获得校验和的16进制数由字符表示
     *
     * @param mbyte
     * @return
     * @throws UnsupportedEncodingException
     */
    static byte[] getCheckSum(byte mbyte) throws UnsupportedEncodingException {
        StringBuilder stringBuilder = new StringBuilder("");
        int value = mbyte & 0xFF; //确保负数也能缩短 转化后的十六进制字符串

        String hexValue = Integer.toHexString(value);//自动忽略16进制数高位的0
        if (hexValue.length()<2){
            stringBuilder.append(0);
        }
        stringBuilder.append(hexValue);
        return stringBuilder.toString().getBytes("ISO-8859-1");
    }
    // 每两个16进制字符表示 解析为 两位十进制字符表示

    /**
     * 每两个16进制字符表示 解析为 最少两位十进制字符表示
     * @param string
     * @return
     */
    static String hexStr2decStr(String string){
        String[] ret = new String[string.length()/2];
        StringBuilder stringBuilderTemp = new StringBuilder("");
        //每两个字符分割成一组
        for(int i =0; i <string.length(); i++){
            if (i%2==0){
                ret[i/2] = string.substring(i,i+2);
            }
        }
        for (String t:ret
        ) {
            String in = Integer.valueOf(t,16).toString();//会把把’00‘转为0 ，这里对应机器的ID 转为00
            if (in.length()<2){
                //System.out.println("<1" + in);
                stringBuilderTemp.append("0" + in);
            }else {
                stringBuilderTemp.append(in);
            }
        }
        return stringBuilderTemp.toString();
    }

    /**解析收到字节数组数据包的校验和，传入字节数组数据包，返回两个字节的十进制表示
     *
     * @param mbyte
     * @return
     */
    static String parseCheckSum(byte[] mbyte){
        byte sum = 0x00;
        //收到的数据包中是一个1024字节数组，必须获得 消息数据包中的长度， 才知道需要计算校验和的是哪些字节。
        int pkg_length = Integer.valueOf(new String(mbyte).substring(1,3),16);
        for(int i=3; i<pkg_length+5; i++){
            sum +=mbyte[i];
        }
        byte checksum =(byte) (~(sum)&0x7F);
        StringBuilder stringBuilder = new StringBuilder("");
        String hexValue = Integer.toHexString(checksum);
        if (hexValue.length()<2){
            stringBuilder.append(0);
        }
        stringBuilder.append(hexValue);
        return stringBuilder.toString();
    }
    //CPU_INT16U 这种发送时内存是两个字节存储的类型，发送到PC端后，因为大小端的原因，组后后字节高低位是逆序的，
    //例如"0x6400",发送过来后应该是"0x0064", 6400是四位 是因为发送的时候一个字节的高低4位，各用一个字节发送，此字节是那四位二进制数对应十进制数的字符
    //例如0010 对应2，那么发的是字符’2‘的字节 十六进制是32
    /***
     * 大小端转化,
     * ****/
    static String exchangeOrder(String str){
        if (str.length()%2 ==1){
            return null;
        }else {
            char[] ma = str.toCharArray();
            StringBuilder stringBuilder = new StringBuilder("");
            for (int i=ma.length-1; i>=0; i=i-2){
                stringBuilder.append(ma[i-1]);
                stringBuilder.append(ma[i]);
            }

            return stringBuilder.toString();
        }
    }
    /*****
     *
     * 读取配置文件
     */
    /**
     * 文件路径
     */
    static String filePath = "identification.properties";

    /**
     * 获取properties文件中的内容,并返回map
     *
     * @return
     */
    static Map<String, byte[]> getProperties() {
        HashMap<String, byte[]> map = new HashMap<String, byte[]>();
        InputStream in = null;
        Properties p = new Properties();;
        try {
            in = new BufferedInputStream(new FileInputStream(new File(filePath)));
            p.load(in);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("读取配置文件异常");
        }
        Set<Map.Entry<Object, Object>> entrySet = p.entrySet();
        for (Map.Entry<Object, Object> entry : entrySet) {
            map.put((String) entry.getKey(), DkssUtil.hexStringToByteArr((String) entry.getValue()));
        }
        // return mConfig;
        return map;
    }
    static void setProperties(HashMap<String,byte[]> configMap){
        mConfig = configMap;
    }
    /*****
     *  验证字节数组对应的16进制数
     */
    static String bytes2hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        String tmp = null;
        for (byte b : bytes) {
            // 将每个字节与0xFF进行与运算，然后转化为10进制，然后借助于Integer再转化为16进制
            tmp = Integer.toHexString(0xFF & b);
            if (tmp.length() == 1) {
                tmp = "0" + tmp;
            }
            sb.append(tmp);
        }
        return sb.toString();

    }

    /*****
     * 解析两个字节的short整型值，两个十六进制数字符转为整型，
     */
    static short hexStr2UnsignedShort(String str){
        return (short)(Integer.parseInt(str.substring(0,2),16)*256 + Integer.parseInt(str.substring(2,4),16));
    }
    /****
     * 一个字节的数据 转为char类型,例如传入 “ef” ，返回 一个字节0xef，char类型表示
     */
    static char hexStr2char(String str){
        return (char)(Integer.valueOf(str,16).intValue());
    }
    /****
     * 一个字节的数据 转为有符号的short
     */
    static short hexStr2SignedShort(String str){
        byte a[] = DkssUtil.hexStringToByteArr(str);
        int a_h = a[0] &0xff;
        int a_l = a[1] &0xff;
        return  (short)((a_h<<8 )|(a_l));
    }

    /***
     * 字节转为二进制编码字符串
     * @param bt
     * @return
     */
//    static String byte2BinaryStr(byte bt){
//        int b =  Byte.toUnsignedInt(bt);
//        String ret = Integer.toBinaryString(b);
//        while (ret.length()<8){
//            ret = "0" + ret;
//        }
//        return  ret;
//    }


}

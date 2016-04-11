package com.example.aya.calculator;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUnderstander;
import com.iflytek.cloud.SpeechUnderstanderListener;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.TextUnderstander;
import com.iflytek.cloud.TextUnderstanderListener;
import com.iflytek.cloud.UnderstanderResult;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Created by aya on 16/2/12.
 */
public class DefineVariablesActivity extends Activity {
    //显示器，用于显示输出结果
    private EditText input;
    private EditText resultText;
    //数字0-9
    private Button[] btn = new Button[10];
    //其他按钮
    private Button  c,  bksp,
            div, left,  mul,
            sub, dot, equal, add, mod, speech_btn;
    //判断是否是按＝之后的输入，true表示输入在＝之前，false反之
    private boolean equals_flag = true;
    //输入控制，true为重新输入，false为接着输入
    private boolean vbegin = true;
    // true表示正确，可以继续输入，false表示有误，输入被锁定
    private boolean tip_lock = true;
    // 控制DRG按键，true为角度，false为弧度
    private boolean drg_flag = true;
    //保存原来的算式样子，为了输出时好看，因计算时算式样子被改变
    private String str_old;
    //变换样子后的式子
    private String str_new;

    // 语义理解对象（语音到语义）。
    private SpeechUnderstander mSpeechUnderstander;

    private SpeechSynthesizer mTts;

    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //加载布局
        setContentView(R.layout.main);

        //获取界面元素
        input = (EditText) findViewById(R.id.input);
        resultText = (EditText) findViewById(R.id.result);
        btn[0] = (Button) findViewById(R.id.zero);
        btn[1] = (Button) findViewById(R.id.one);
        btn[2] = (Button) findViewById(R.id.two);
        btn[3] = (Button) findViewById(R.id.three);
        btn[4] = (Button) findViewById(R.id.four);
        btn[5] = (Button) findViewById(R.id.five);
        btn[6] = (Button) findViewById(R.id.six);
        btn[7] = (Button) findViewById(R.id.seven);
        btn[8] = (Button) findViewById(R.id.eight);
        btn[9] = (Button) findViewById(R.id.nine);
        c = (Button) findViewById(R.id.c);

        bksp = (Button) findViewById(R.id.bksp);
        div = (Button) findViewById(R.id.divide);
        left = (Button) findViewById(R.id.left);

        mul = (Button) findViewById(R.id.mul);

        sub = (Button) findViewById(R.id.sub);
        dot = (Button) findViewById(R.id.dot);
        equal = (Button) findViewById(R.id.equal);
        add = (Button) findViewById(R.id.add);
        mod = (Button) findViewById(R.id.mod);
        speech_btn = (Button) findViewById(R.id.speech_button);

        //注册点击事件
        for (int i = 0; i < 10; ++i) {
            btn[i].setOnClickListener(actionPerformed);
        }
        c.setOnClickListener(actionPerformed);
        bksp.setOnClickListener(actionPerformed);
        div.setOnClickListener(actionPerformed);
        left.setOnClickListener(actionPerformed);
        mul.setOnClickListener(actionPerformed);
        sub.setOnClickListener(actionPerformed);
        dot.setOnClickListener(actionPerformed);
        equal.setOnClickListener(actionPerformed);
        add.setOnClickListener(actionPerformed);
        mod.setOnClickListener(actionPerformed);
        speech_btn.setOnClickListener(actionSpeech);

        SpeechUtility.createUtility(getBaseContext(), SpeechConstant.APPID + "=565d5e14");
        // 初始化对象
        mSpeechUnderstander = SpeechUnderstander.createUnderstander(DefineVariablesActivity.this, mSpeechUdrInitListener);

        mTts = SpeechSynthesizer.createSynthesizer(DefineVariablesActivity.this, null);
    }

    /**
     * 初始化监听器（语音到语义）。
     */
    private InitListener mSpeechUdrInitListener = new InitListener() {

        @Override
        public void onInit(int code) {

            if (code != ErrorCode.SUCCESS) {
                Log.v("error", "speechUnderstanderListener init() code = " + code);
            }
        }
    };

    private int[]  getBracketNum(String inputString){
        int leftnum = 0;
        int rightnum = 0;
        String inputStrings[] = inputString.split("");
        String leftBrack = "(";
        String rightBrack = ")";
        for(int i =0;i < inputStrings.length;i++){
            if (inputStrings[i].equals(leftBrack)){
                leftnum++;
            } else if(inputStrings[i].equals(rightBrack)){
                rightnum++;
            }
        }
        int[] bracketnums = {leftnum,rightnum};
        return bracketnums;
    }

    private RecognizerListener mRecognizerListener = new RecognizerListener() {
        // 音量变化
        public void onVolumeChanged(int volume, byte[] data) {}
        // 返回结果
        public void onResult(final RecognizerResult result, boolean isLast) {} // 开始说话
        public void onBeginOfSpeech() {}
        // 结束说话
        public void onEndOfSpeech() {}
        // 错误回调
        public void onError(SpeechError error) {}
        // 事件回调
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {}
    };

    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener(){
        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            Log.v("result",recognizerResult.getResultString());
            String text = recognizerResult.getResultString();
            Log.v("result", text);
            Map<String,Object> resultMap = getMapForJson(text);
            if(resultMap.get("answer") != null){
                JSONObject answerJsonObject =(JSONObject)resultMap.get("answer");
                String resultString = null;
                try {
                    resultString = (String) answerJsonObject.get("text");
                    mTts.startSpeaking(resultString, null);
                    String showText = resultString.substring(2);
                    showText = "=" + showText;
                    resultText.setText(showText);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }

        @Override
        public void onError(SpeechError speechError) {

        }
    };

    public static Map<String, Object> getMapForJson(String jsonStr){
        JSONObject jsonObject ;
        try {
            jsonObject = new JSONObject(jsonStr);

            Iterator<String> keyIter= jsonObject.keys();
            String key;
            Object value ;
            Map<String, Object> valueMap = new HashMap<String, Object>();
            while (keyIter.hasNext()) {
                key = keyIter.next();
                value = jsonObject.get(key);
                valueMap.put(key, value);
            }
            return valueMap;
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();

        }
        return null;
    }

    private SpeechUnderstanderListener mUnderstanderListener = new SpeechUnderstanderListener(){
        public void onResult(UnderstanderResult result) {
            String text = result.getResultString();
            Log.v("result", text);
            Map<String,Object> resultMap = getMapForJson(text);
            if(resultMap.get("answer") != null){
                JSONObject answerJsonObject =(JSONObject)resultMap.get("answer");
                String resultString = null;
                try {
                    resultString = (String) answerJsonObject.get("text");
                    mTts.startSpeaking(resultString, null);
                    String showText = resultString.substring(2);
                    showText = "=" + showText;
                    resultText.setText(showText);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
        public void onError(SpeechError error) {}//会话发生错误回调接口
        public void onBeginOfSpeech() {
            Log.v("result","onBeginOfSpeech");
        }//开始录音
        public void onVolumeChanged(int volume, byte[] data){} //volume音量值0~30,data音频数据 public void onEndOfSpeech() {}//结束录音
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {}//扩展用接口

        @Override
        public void onEndOfSpeech() {
            Log.v("result","onEndOfSpeech");
        }
    };

    public void setParam(){
        // 设置语言
        mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mSpeechUnderstander.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mSpeechUnderstander.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/sud.wav");
    }

    int ret = 0;// 函数调用返回值

    private View.OnClickListener actionSpeech = new View.OnClickListener(){
        @Override
        public void onClick(View v){
            setParam();
//            ret = mSpeechUnderstander.startUnderstanding(mUnderstanderListener);
//            if(ret != 0){
//                Log.v("error:","语义理解失败,错误码:"	+ ret);
//            }else {
//                Log.v("begin:","begin"	+ ret);
//            }
//            SpeechUtility.createUtility(getBaseContext(), SpeechConstant.APPID + "=5703ca03");
//            TextUnderstanderListener searchListener = new TextUnderstanderListener(){
//                //语义结果回调
//                public void onResult(UnderstanderResult result){
//                    Log.v("result :",result.getResultString());
//                } //语义错误回调
//                public void onError(SpeechError error) {}
//            };
//            //创建文本语义理解对象
//            TextUnderstander mTextUnderstander = TextUnderstander.createTextUnderstander(DefineVariablesActivity.this, null); //开始语义理解
//            mTextUnderstander.understandText("7+8等于几", searchListener);
//初始化监听器


// 识别监听器
            SpeechUtility.createUtility(getBaseContext(), SpeechConstant.APPID + "=5703ca03");
            RecognizerDialog mDialog = new RecognizerDialog(DefineVariablesActivity.this, new InitListener(){
                @Override
                public void onInit(int i) {

                }
            }); //2.设置accent、language等参数
            mDialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn"); mDialog.setParameter(SpeechConstant.ACCENT, "mandarin");
            //若要将UI控件用于语义理解,必须添加以下参数设置,设置之后onResult回调返回将是语义理解 //结果
            mDialog.setParameter(SpeechConstant.DOMAIN, "iat");
            mDialog.setParameter(SpeechConstant.RESULT_TYPE, "json");
            mDialog.setParameter(SpeechConstant.NLP_VERSION, "2.0");
            mDialog.setParameter(SpeechConstant.PARAMS , "sch=1");
//3.设置回调接口
            mDialog.setListener(mRecognizerDialogListener);

            //4.显示dialog,接收语音输入
            mDialog.show();

        }
    };


    /*
        *键盘命令捕捉
         */
    //命令缓存，用于检测输入合法性
    private String[] tipCommand = new String[500];
    private int tip_i = 0;
    private View.OnClickListener actionPerformed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //获取按键上的内容
            String command = ((Button) v).getText().toString();
            //获取显示器上的字符串
            String str = input.getText().toString();
            //检测输入是否合法，判断所按的键是否在按＝之后，是否为运算符
            if (equals_flag == false && "0123456789.()sincostanlnlogn!+-×÷√^%".indexOf(command) != -1) {
                //检测显示器上的字符串是否合法
                if (right(str)) {
                    if ("+-×÷√^%".indexOf(command) != -1) {
                        for (int i = 0; i < str.length(); i++) {
                            tipCommand[tip_i] = String.valueOf(str.charAt(i));
                            tip_i++;
                        }
                        vbegin = false;
                    }
                } else {
                    input.setText("0");
                    vbegin = true;
                    tip_i = 0;
                    tip_lock = true;

                }
                equals_flag = true;
            }
            // 将缓存字符串的最后一位和当前输入命令进行分析
            if (tip_i > 0)
                tipChecker(tipCommand[tip_i - 1],command);
            else if (tip_i == 0) tipChecker("#", command);
            if ("0123456789.()sincostanlnlogn!+-×÷√^%".indexOf(command) != -1 && tip_lock) {
                tipCommand[tip_i] = command;
                tip_i++;
            }

            //若输入正确，则将输入信息显示在显示器上
            if ("0123456789.sincostanlnlogn!+-×÷√^%".indexOf(command) != -1 && tip_lock) {
                print(command);
                // 如果单击来DRg，则切换当前弧度角度制并将切换后的结果显示到按键上方
            } else if (command.compareTo("DRG") == 0 && tip_lock) {
                if (drg_flag == true) {
                    drg_flag = false;
                } else {
                    drg_flag = true;
                }
                //如果输入的是退格键，并且是在按＝之前
            } else if (command.compareTo("Bksp") == 0 && equals_flag) {
                if (tto(str) == 3) {
                    if (str.length() > 3)
                        input.setText(str.substring(0, str.length() - 3));
                    else if (str.length() == 3) {
                        input.setText("0");
                        vbegin = true;
                        tip_i = 0;

                    }
                    //一次删除2个字符
                } else if (tto(str) == 2) {
                    if (str.length() > 2)
                        input.setText(str.substring(0, str.length() - 2));
                    else if (str.length() == 2) {
                        input.setText("0");
                        vbegin = true;
                        tip_i = 0;

                    }
                    // 一次删除一个字符
                } else if (tto(str) == 1) {
                    //若之前输入的字符串合法，则删除一个字符
                    if (right(str)) {
                        if (str.length() > 1) {
                            input.setText(str.substring(0, str.length()- 1));
                        } else if (str.length() == 1) {
                            input.setText("0");
                            vbegin = true;
                            tip_i = 0;

                        }
                        //若之前输入的字符串不合法，则删除全部字符
                    } else {
                        input.setText("0");
                        vbegin = true;
                        tip_i = 0;

                    }
                }
                if (input.getText().toString().compareTo("-") == 0 || equals_flag == false) {
                    input.setText("0");
                    vbegin = true;
                    tip_i = 0;

                }
                tip_lock = true;
                if (tip_i > 0)
                    tip_i--;
                //如果是在按=之后输入退格键
            } else if (command.compareTo("Bksp") == 0 && equals_flag == false) {
                //将显示器内容设置为0
                input.setText("0");
                vbegin = true;
                tip_i = 0;
                tip_lock = true;

                //如果输入的清除键
            } else if (command.compareTo("C") == 0) {
                //将显示器内容设置为0
                input.setText("0");
                vbegin = true;
                tip_i = 0;
                tip_lock = true;
                //表示在输入=之前
                equals_flag = true;
                //如果输入的是mc，则将存储器内容清0
            } else if (command.compareTo("MC") == 0) {
                //如果按exit则退出程序
            } else if (command.compareTo("exit") == 0) {
                System.exit(0);
                //如果输入的是=，并且输入合法
            } else if (command.compareTo("()") == 0){
                int[] bracketNums =getBracketNum(input.getText().toString());
                if (bracketNums[0] > bracketNums[1]){
                    print(")");
                } else {
                    print("(");
                }
            } else if (command.compareTo("=") == 0 && tip_lock && right(str) && equals_flag) {
                tip_i = 0;
                tip_lock = false;
                equals_flag = false;
                str_old = str;
                //替换算式中的运算符，便于计算
                str = str.replaceAll("sin", "s");
                str = str.replaceAll("cos", "c");
                str = str.replaceAll("tan", "t");
                str = str.replaceAll("log", "g");
                str = str.replaceAll("ln", "l");
                str = str.replaceAll("n!", "!");
                //重新输入标志换为true
                vbegin = true;
                str_new = str.replaceAll("-", "-1×");
                //计算算式结果
                process(str_new);
            }
            //表示可以继续输入
            tip_lock = true;
        }
    };

    //向input输出字符
    private void print(String str) {
        if (vbegin) {
            //清屏后输出
            input.setText(str);
        } else {
            input.append(str);
        }
        vbegin = false;
    }

    /*
    *判断一个str是否合法，返回值为true,false
    * 只包含0123456789.()sincostanlnlogn!+-×÷√^的是合法的str，返回true
    * 包含了除0123456789.()sincostanlnlogn!+-×÷√^以外的字符的str为非法的，返回false
     */
    private boolean right(String str) {
        int i;
        for (i = 0; i < str.length(); i++) {
            if (str.charAt(i) != '0' && str.charAt(i) != '1' && str.charAt(i) != '2' && str.charAt(i) != '3' && str.charAt(i) != '4'
                    && str.charAt(i) != '5' && str.charAt(i) != '6' && str.charAt(i) != '7' && str.charAt(i) != '8' && str.charAt(i) != '9'
                    && str.charAt(i) != '.' && str.charAt(i) != '(' && str.charAt(i) != ')' && str.charAt(i) != 's' && str.charAt(i) != 'i'
                    && str.charAt(i) != 'n' && str.charAt(i) != 'c' && str.charAt(i) != 'o' && str.charAt(i) != 't' && str.charAt(i) != 'a'
                    && str.charAt(i) != 'l' && str.charAt(i) != 'g' && str.charAt(i) != '!' && str.charAt(i) != '+' && str.charAt(i) != '-' && str.charAt(i) != '×'
                    && str.charAt(i) != '÷' && str.charAt(i) != '√' && str.charAt(i) != '^' && str.charAt(i) != '%')
                break;
        }
        if (i == str.length()) {
                return true;
        } else {
                return false;
        }
    }

    /*
    * 检测函数，返回值为1，2，3，表示一次应当删除几个字符，为BKSP按钮的删除方式提供依据
    *返回3，表示str尾部为sin,cos,tan,log中的一个，应当一次删除3个
    *返回2，表示str尾部为ln，n！中的一个，应当一次删除2个
    *返回1，表示str尾部为其他情况，应当一次删除1个（包含非法字符时另外考虑，应当清0）
     */
    private int tto(String str){
        if(str.charAt(str.length()-1)=='n'&&str.charAt(str.length()-2)=='i'&&str.charAt(str.length()-3)=='s'
        ||str.charAt(str.length()-1)=='s'&&str.charAt(str.length()-2)=='o'&&str.charAt(str.length()-3)=='c'
            ||str.charAt(str.length()-1)=='n'&&str.charAt(str.length()-2)=='a'&&str.charAt(str.length()-3)=='t'
                ||str.charAt(str.length()-1)=='g'&&str.charAt(str.length()-2)=='o'&&str.charAt(str.length()-3)=='l') {
            return 3;
        }else if(str.charAt(str.length()-1)=='n'&&str.charAt(str.length()-2)=='l'
                ||str.charAt(str.length()-1)=='!'&&str.charAt(str.length()-2)=='n'){
            return 2;
        }else {
            return 1;
        }
    }

     /*检测函数，对str进行前后语法检测
     为tip的提示方式提供依据，与tipshow（）配合使用
      */
    private void tipChecker(String tipcommand1,String tipcommand2){
        //tipcode1表述错误类型，tipcode2表示名词解释类型
        int tipCode1=0;
        int tipCode2=0;
        //表示命令类型
        int tipType1=0;
        int tipType2=0;
        // 括号数
        int bracket=0;
        //"+×÷)√￣^"不能作为第一位
        if(tipcommand1.compareTo("#")==0&&(tipcommand2.compareTo("+")==0||tipcommand2.compareTo("×")==0
        ||tipcommand2.compareTo("÷")==0||tipcommand2.compareTo(")")==0||tipcommand2.compareTo("√")==0
        ||tipcommand2.compareTo("^")==0)){
            tipCode1=-1;
        }//定义存储字符串中最后一位的类型
        else if(tipcommand1.compareTo("#")!=0){
            if(tipcommand1.compareTo("(")==0){
                tipType1=1;
            }else if(tipcommand1.compareTo(")")==0){
                tipType1=2;
            }else if(tipcommand1.compareTo(".")==0){
                tipType1=3;
            }else if("0123456789".indexOf(tipcommand1)!=-1){
                tipType1=4;
            }else if("+-×÷".indexOf(tipcommand1)!=-1){
                tipType1=5;
            }else if("√￣^".indexOf(tipcommand1)!=-1){
                tipType1=6;
            }else if("sincostanlnlogn!".indexOf(tipcommand1)!=-1){
                tipType1=7;
            }
            //定义欲输入的按键类型
            if(tipcommand2.compareTo("(")==0){
                tipType2=1;
            }else if(tipcommand2.compareTo(")")==0){
                tipType2=2;
            }else if(tipcommand2.compareTo(".")==0){
                tipType2=3;
            }else if("0123456789".indexOf(tipcommand2)!=-1){
                tipType2=4;
            }else if("+-×÷".indexOf(tipcommand2)!=-1){
                tipType2=5;
            }else if("√^".indexOf(tipcommand2)!=-1){
                tipType2=6;
            }else if("sincostanlnlogn!".indexOf(tipcommand2)!=-1){
                tipType2=7;
            }
            switch (tipType1){
                case 1:
                    //左括号后面直接接右括号，“+×÷”，或者√^时为错误类型
                    if(tipType2==2||(tipType2==5&&tipcommand2.compareTo("-")!=0)||tipType2==6)
                        tipCode1=1;
                    break;
                case 2:
                    //右括号后面接左括号，数字，.,函数时为错误类型
                    if(tipType2==1||tipType2==3||tipType2==4||tipType2==7)
                        tipCode1=2;
                    break;
                case 3:
                    //.后面接（，或函数，则为错误类型
                    if(tipType2==1||tipType2==7)
                        tipCode1=3;
                    //.后面接.
                    if(tipType2==3)
                        tipCode1=8;
                    break;
                case 4:
                    //数字后面接（，函数，则为错误类型
                    if(tipType2==1||tipType2==7)
                        tipCode1=4;
                    break;
                case 5:
                    //算符后面接）或算符，√^为错误类型
                    if(tipType2==2||tipType2==5||tipType2==6)
                        tipCode1=5;
                    break;
                case 6:
                    //√^后面接），算符，√^，函数，为错误类型
                    if(tipType2==2||tipType2==5||tipType2==6||tipType2==7)
                        tipCode1=6;
                    break;
                case 7:
                    //函数后面接），算符，√^，函数，为错误类型
                    if(tipType2==2||tipType2==5||tipType2==6||tipType2==7)
                        tipCode1=7;
                    break;
            }
        }

        //检测小数点个数
        if(tipCode1==0&&tipcommand2.compareTo(".")==0){
            int tip_point=0;
            for(int i=0;i<tip_i;i++){
                //若之前出现一个小数点，则小数点计数加1
                if(tipCommand[i].compareTo(".")==0){
                    tip_point++;
                }
                //若出现以下几个运算符之一，小数点计数就清0
                if(tipCommand[i].compareTo("sin")==0||tipCommand[i].compareTo("cos")==0
                        ||tipCommand[i].compareTo("tan")==0||tipCommand[i].compareTo("ln")==0
                        ||tipCommand[i].compareTo("log")==0||tipCommand[i].compareTo("n!")==0
                        ||tipCommand[i].compareTo("+")==0||tipCommand[i].compareTo("-")==0
                        ||tipCommand[i].compareTo("×")==0||tipCommand[i].compareTo("÷")==0
                        ||tipCommand[i].compareTo("√")==0||tipCommand[i].compareTo("^")==0)
                {
                    tip_point=0;
                }
            }
            tip_point++;
            //若小数点计数大于1，表示小数点重复了
            if(tip_point>1)
            {
                tipCode1=8;
            }
        }


        //检测左右括号个数是否匹配
        if(tipCode1==0&&tipcommand2.compareTo(")")==0){
            int tip_right_bracket=0;
            for(int i=0;i<tip_i;i++){
                // 如果出现一个左括号，则计数加1
                if(tipCommand[i].compareTo("(")==0){
                    tip_right_bracket++;
                }
                //如果出现一个右括号，则计数减1
                if(tipCommand[i].compareTo(")")==0){
                    tip_right_bracket--;
                }
            }
            //若果计数为0，表示没有相应的左括号与输入的右括号匹配
            if(tip_right_bracket==0){
                tipCode1=10;
            }
        }


        //检查输入=的合法性
        if(tipCode1==0&&tipcommand2.compareTo("=")==0){
            //括号匹配数
            int tip_bracket=0;
            for(int i=0;i<tip_i;i++){
                if(tipCommand[i].compareTo("(")==0){
                    tip_bracket++;
                }
                if(tipCommand[i].compareTo(")")==0){
                    tip_bracket--;
                }
            }
            //若大于0，表示左括号还有未匹配
            if(tip_bracket>0){
                tipCode1=9;
                bracket=tip_bracket;
            }else if(tip_bracket==0){
                //若前一个字符是以下之一，表示=不合法
                if("√^sincostanlnlogn!".indexOf(tipcommand1)!=-1){
                    tipCode1=6;
                }
                if("+-×÷".indexOf(tipcommand1)!=-1){
                    tipCode1=5;
                }
            }
        }

        //若命令是以下之一，则显示相应的帮助信息
        if(tipcommand2.compareTo("MC")==0)  tipCode2=1;
        if(tipcommand2.compareTo("C")==0)  tipCode2=2;
        if(tipcommand2.compareTo("DRG")==0)  tipCode2=3;
        if(tipcommand2.compareTo("Bksp")==0)  tipCode2=4;
        if(tipcommand2.compareTo("sin")==0)  tipCode2=5;
        if(tipcommand2.compareTo("cos")==0)  tipCode2=6;
        if(tipcommand2.compareTo("tan")==0)  tipCode2=7;
        if(tipcommand2.compareTo("ln")==0)  tipCode2=8;
        if(tipcommand2.compareTo("log")==0)  tipCode2=9;
        if(tipcommand2.compareTo("n!")==0)  tipCode2=10;
        if(tipcommand2.compareTo("√")==0)  tipCode2=11;
        if(tipcommand2.compareTo("^")==0)  tipCode2=12;
        //显示帮助和错误信息
        tipShow(bracket,tipCode1,tipCode2,tipcommand1,tipcommand2);
    }



    /*
    *反馈tip信息，加强人机交互
    */
    private void tipShow(int bracket,int tipCode1,int tipCode2,String tipCommand1,String tipCommand2 ){
        String tipMessage="";
        if(tipCode1!=0)
            tip_lock=false;    //表示输入有误
        switch(tipCode1){
            case -1:
                tipMessage=tipCommand2+" 不能作为第一个算符\n";
                break;
            case 1:
                tipMessage=tipCommand1+" 后应输入：数字/(/./-/函数\n";
                break;
            case 2:
                tipMessage=tipCommand1+" 后应输入：)/算符\n";
                break;
            case 3:
                tipMessage=tipCommand1+"  后应输入：)/数字/算符\n";
                break;
            case 4:
                tipMessage=tipCommand1+" 后应输入：)/./数字/算符\n";
                break;
            case 5:
                tipMessage=tipCommand1+" 后应输入：(/./数字/函数\n";
                break;
            case 6:
                tipMessage=tipCommand1+" 后应输入：(/./数字\n";
                break;
            case 7:
                tipMessage=tipCommand1+" 后应输入：(/./数字\n";
                break;
            case 8:
                tipMessage=" 小数点重复\n";
                break;
            case 9:
                tipMessage=" 不能计算，缺少"+bracket+"个)";
                break;
            case 10:
                tipMessage=" 不需要）";
                break;
        }

        switch(tipCode2){
            case 1:
                tipMessage=tipMessage+"[MC 用法:清楚记忆 MEM]";
                break;
            case 2:
                tipMessage=tipMessage+"[C 用法:归零]";
                break;
            case 3:
                tipMessage=tipMessage+"[DRG 选择DRG 或RAD]";
                break;
            case 4:
                tipMessage=tipMessage+"[Bksp 用法:退格]";
                break;
            case 5:
                tipMessage=tipMessage+"sin函数用法示例：\n"+
                        "DEG:sin30=0.5  RAD:sin1=0.84\n"+
                        "注：与其他函数一起使用时要加括号，如:\n"+
                        "sin(cos45),而不是sincos45";
                break;
            case 6:
                tipMessage=tipMessage+"cos函数用法示例：\n"+
                        "DEG:cos60=0.5  RAD:cos1=0.54\n"+
                        "注：与其他函数一起使用时要加括号，如:\n"+
                        "cos(sin45),而不是cossin45";
                break;
            case 7:
                tipMessage=tipMessage+"tan函数用法示例：\n"+
                        "DEG:tan45=1  RAD:tan1=1.55\n"+
                        "注：与其他函数一起使用时要加括号，如:\n"+
                        "tan(cos45),而不是tanos45";
                break;
            case 8:
                tipMessage=tipMessage+"log函数用法示例：\n"+
                        "log10=log(5+5)=1\n"+
                        "注：与其他函数一起使用时要加括号，如:\n"+
                        "log(tan45),而不是logtan45";
                break;
            case 9:
                tipMessage=tipMessage+"ln函数用法示例：\n"+
                        "ln10=le(5+5)=2.3   lne=1\n"+
                        "注：与其他函数一起使用时要加括号，如:\n"+
                        "ln(tan45),而不是lntan45";
                break;
            case 10:
                tipMessage=tipMessage+"n!函数用法示例：\n"+
                        "n!3=n!(1+2)=3*2*1=6\n"+
                        "注：与其他函数一起使用时要加括号，如:\n"+
                        "n!(log1000),而不是n!log1000";
                break;
            case 11:
                tipMessage=tipMessage+"√用法示例：开任意次根号\n"+
                        "如：27开3次根为27√3=3\n"+
                        "注：与其他函数一起使用时要加括号，如:\n"+
                        "（函数）√(函数),（n!3）√(log100)=2.45";
                break;
            case 12:
                tipMessage=tipMessage+"^用法示例：开任意次平方\n"+
                        "如：2的3次方为 2^3=8\n"+
                        "注：与其他函数一起使用时要加括号，如:\n"+
                        "（函数）^(函数),（n!3）^(log100)=36";
                break;
        }
    }


/*
 *整个计算核心，只要将表达式的整个字符串传入Calc.process()就可以实行计算了
 * 算法包括以下几个部分
 * 1.计算部分 process(String str)当然，这是建立在查找无错的情况下
 * 2.数据格式化 fp(double n) 使数据有相当的精确度
 * 3.阶乘算法 n(double n) 计算n！，将结果返回
 * 4.错误提示 showError(int code, String str)  将错误返回
 */

        private final int MAXLEN=500;
        private double pi=4*Math.atan(1);
        /*
        *从左到右扫描，数字入number栈，运算入operator栈
        *＋－基本优先级为1，×÷基本优先级为2，sincostanlnlogn!基本优先级为3，√^基本优先级为4
        * 括号内层运算符比外层同级运算符优先级高4
        * 当前运算符优先级高于栈顶压栈，低于栈顶弹出一个运算符与两个数进行运算，重复直到当前运算符大于栈顶
        * 扫描完后对剩下的运算符与数字依次计算
         */
        private void process(String str){

            int weightPlus=0;   //同一（）下的基本优先级
            int topOp=0;     //为weight[] operator[]的计数器
            int topNum=0;     //为number[]的计数器
            int flag=1;      //为正负数的计数器，1为正，-1为负
            int weightTemp=0;    //临时记录优先级的变化
            int weight[]=new int[MAXLEN];
            double number[]=new double[MAXLEN];
            char operator[]=new char[MAXLEN];  //保存运算符，以topOp计数
            char ch='0';
            char ch_gai='0';
            String num=null;  //记录分段后的数字
            String expression=str;
            StringTokenizer expToken=new StringTokenizer(expression,"()sctlg!+-×÷√^%");
            int i=0;
            while(i<expression.length()){
                ch=expression.charAt(i);
                //判断正负数
                if(i==0){
                    if(ch=='-')
                        flag=-1;
                }else if(expression.charAt(i-1)=='('&&ch=='-')
                    flag=-1;
                //取得数字，并将符号转移给数字,i定位在最后的数字位
                if(ch>='0'&&ch<='9'||ch=='.'||ch=='E'){
                    num=expToken.nextToken();
                    ch_gai=ch;
                    Log.e("guojs", ch + "---->" + i);
                    while(i<expression.length()&&(ch_gai>='0'&&ch_gai<='9'||ch_gai=='.'||ch_gai=='E')){
                        ch_gai=expression.charAt(i++);
                        Log.e("guojs","i的值为"+i);
                    }
                    if(i>=expression.length()) i-=1;  else i-=2;
                    //将正负符号转移给数字
                    if(num.compareTo(".")==0)  number[topNum++]=0;
                    else {
                        number[topNum++]=Double.parseDouble(num)*flag;
                        flag=1;
                    }
                }

                //计算运算符的优先级
                if(ch=='(') weightPlus+=4;
                if(ch==')') weightPlus-=4;
                if(ch=='-'&&flag==1||ch=='+'||ch=='×'||ch=='÷'||ch=='s'||ch=='c'||ch=='t'||ch=='g'||ch=='l'
                        ||ch=='!'||ch=='√'||ch=='^'||ch=='%'){
                    switch (ch){
                        //+-的优先级最低，为1
                        case '+':
                        case '-':
                            weightTemp=1+weightPlus;
                            break;
                        //÷×的优先级稍高，为2
                        case '÷':
                        case '×':
                        case '%':
                            weightTemp=2+weightPlus;
                            break;
                        //sctlg!优先级为3
                        case 's':
                        case 'c':
                        case 't':
                        case 'g':
                        case 'l':
                        case '!':
                            weightTemp=3+weightPlus;
                            break;
                        case '√':
                        case '^':
                            weightTemp=4+weightPlus;
                            break;
                    }
                    //如果当前优先级大于栈顶部元素则直接入栈
                    if(topOp==0||weightTemp>weight[topOp-1])
                    {
                        weight[topOp]=weightTemp;
                        operator[topOp]=ch;
                        topOp++;
                        //否则将堆栈中的运算符逐个取出，直到当前堆栈顶部运算符优先级小于当前运算符
                    }else {
                        while (topOp>0&&weight[topOp-1]>=weightTemp){
                            switch (operator[topOp-1]){
                                //取出数字数组的相应元素进行计算
                                case '+':
                                    number[topNum-2]+=number[topNum-1];
                                    break;
                                case '-':
                                    number[topNum-2]-=number[topNum-1];
                                    break;
                                case '×':
                                    number[topNum-2]*=number[topNum-1];
                                    break;
                                case '%':
                                    number[topNum-2]%=number[topNum-1];
                                    break;
                                //判断除数为0的情况
                                case '÷':
                                    if(number[topNum-1]==0){
                                        showError(1,str_old);
                                        return;
                                    }
                                    number[topNum-2]/=number[topNum-1];
                                    break;
                                case '√':
                                    if(number[topNum-1]==0||(number[topNum-2]<0&&number[topNum-1]%2==0)){
                                        showError(2,str_old);
                                        return;
                                    }
                                    number[topNum-2]=Math.pow(number[topNum-2],1/number[topNum-1]);
                                    break;
                                case '^':
                                    number[topNum-2]=Math.pow(number[topNum-2],number[topNum-1]);
                                    break;
                                //计算时进行角度弧度的判断及转换
                                //sin
                                case 's':
                                    if(drg_flag==true){
                                        number[topNum-1]=Math.sin((number[topNum - 1] / 180) * pi);
                                    }else{
                                        number[topNum-1]=Math.sin(number[topNum - 1]);
                                    }
                                    topNum++;
                                    break;
                                //cos
                                case 'c':
                                    if(drg_flag==true){
                                        number[topNum-1]=Math.cos((number[topNum - 1] / 180) * pi);
                                    }else{
                                        number[topNum-1]=Math.cos(number[topNum - 1]);
                                    }
                                    topNum++;
                                    break;
                                //tan
                                case 't':
                                    if(drg_flag==true){
                                        if(Math.abs(number[topNum-1])/90%2==1){
                                            showError(2,str_old);
                                            return;
                                        }
                                        number[topNum-1]=Math.tan((number[topNum - 1] / 180) * pi);
                                    }else{
                                        if(Math.abs(number[topNum-1])/(pi/2)%2==1) {
                                            showError(2, str_old);
                                            return;
                                        }
                                        number[topNum-1]=Math.tan(number[topNum - 1]);
                                    }
                                    topNum++;
                                    break;
                                //log
                                case 'g':
                                    if(number[topNum-1]<=0) {
                                        showError(2, str_old);
                                        return;
                                    }
                                    number[topNum-1]=Math.log(number[topNum - 1]);
                                    topNum++;
                                    break;
                                //ln
                                case 'l':
                                    if(number[topNum-1]<=0) {
                                        showError(2, str_old);
                                        return;
                                    }
                                    number[topNum-1]=Math.log10(number[topNum - 1]);
                                    topNum++;
                                    break;
                                //n!
                                case '!':
                                    if(number[topNum-1]>170) {
                                        showError(3, str_old);
                                        return;
                                    }else if(number[topNum-1]<0){
                                        showError(2, str_old);
                                        return;
                                    }
                                    number[topNum-1]=n(number[topNum - 1]);
                                    topNum++;
                                    break;
                            }
                            //继续取堆栈的下一个元素进行判断
                            topNum--;
                            topOp--;
                        }
                        //当不满足条件时，将运算符压入堆栈
                        weight[topOp]=weightTemp;
                        operator[topOp]=ch;
                        topOp++;
                    }
                }
                i++;
            }

            //依次取出堆栈的运算符进行运算
            while (topOp>0){
                //＋－＊直接将数组的后两位数取出运算
                switch (operator[topOp-1]){
                    case '+':
                        number[topNum-2]+=number[topNum-1];
                        break;
                    case '-':
                        number[topNum-2]-=number[topNum-1];
                        break;
                    case '×':
                        number[topNum-2]*=number[topNum-1];
                        break;
                    //涉及到除法时要考虑到除数不能为0的情况
                    case '÷':
                        if(number[topNum-1]==0){
                            showError(1,str_old);
                            return;
                        }
                        number[topNum-2]/=number[topNum-1];
                        break;
                    case '√':
                        if(number[topNum-1]==0||(number[topNum-2]<0&&number[topNum-1]%2==0)){
                            showError(2,str_old);
                            return;
                        }
                        number[topNum-2]=Math.pow(number[topNum-2],1/number[topNum-1]);
                        break;
                    case '^':
                        number[topNum-2]=Math.pow(number[topNum-2],number[topNum-1]);
                        break;
                    //sin
                    case 's':
                        if(drg_flag==true){
                            number[topNum-1]=Math.sin((number[topNum - 1] / 180) * pi);
                        }else{
                            number[topNum-1]=Math.sin(number[topNum - 1]);
                        }
                        topNum++;
                        break;
                    //cos
                    case 'c':
                        if(drg_flag==true){
                            number[topNum-1]=Math.cos((number[topNum - 1] / 180) * pi);
                        }else{
                            number[topNum-1]=Math.cos(number[topNum - 1]);
                        }
                        topNum++;
                        break;
                    //tan
                    case 't':
                        if(drg_flag==true){
                            if(Math.abs(number[topNum-1])/90%2==1){
                                showError(2,str_old);
                                return;
                            }
                            number[topNum-1]=Math.tan((number[topNum - 1] / 180) * pi);
                        }else{
                            if(Math.abs(number[topNum-1])/(pi/2)%2==1) {
                                showError(2, str_old);
                                return;
                            }
                            number[topNum-1]=Math.tan(number[topNum - 1]);
                        }
                        topNum++;
                        break;
                    //log
                    case 'g':
                        if(number[topNum-1]<=0) {
                            showError(2, str_old);
                            return;
                        }
                        number[topNum-1]=Math.log(number[topNum - 1]);
                        topNum++;
                        break;
                    //ln
                    case 'l':
                        if(number[topNum-1]<=0) {
                            showError(2,str_old);
                            return;
                        }
                        number[topNum-1]=Math.log10(number[topNum - 1]);
                        topNum++;
                        break;
                    //n!
                    case '!':
                        if(number[topNum-1]>170) {
                            showError(3, str_old);
                            return;
                        }else if(number[topNum-1]<0){
                            showError(2, str_old);
                            return;
                        }
                        number[topNum-1]=n(number[topNum - 1]);
                        topNum++;
                        break;
                    //%
                    case '%':
                        number[topNum-2]%=number[topNum-1];
                        break;
                }
                //取堆栈下一个元素进行运算
                topNum--;
                topOp--;
            }

            //如果数字太大，提示错误信息
            if(number[0]>7.3E306){
                showError(3, str_old);
                return;
            }

            //输出最终结果
//            input.setText(String.valueOf(fP(number[0])));
            String resultString = "="+String.valueOf(fP(number[0]));
            resultText.setText(resultString);
        }

        /*控制小数点位数，达到精度
        *本格式进度为15位
         */
        private double fP(double n){
            DecimalFormat format=new DecimalFormat("0.#############");
            return Double.parseDouble(format.format(n));
        }

        /*
        *阶乘算法
         */
        private double n(double n){
            double sum=1;
            //依次将小于等于n的值相乘
            for(int i=1;i<=n;i++){
                sum=sum*i;
            }
            return sum;
        }

        /*
        *错误提示，按＝之后，若计算式在process过程中出现错误，则提示
         */
        private void showError(int code,String str){
            String message="";
            switch (code){
                case 1:
                    message="0不能作为除数";
                    break;
                case 2:
                    message="函数格式错误";
                    break;
                case 3:
                    message="值太大，超出范围";
            }
            input.setText("\""+str+"\""+":"+message);

        }





}




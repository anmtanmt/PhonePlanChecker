package jp.anmt.phoneplanchecker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

//    Context mContext = null;

    private boolean mDoAnalyze = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Param.D) Log.d(TAG, "onCreate()");

//        mContext = this;

        setLayout();
    }

    @Override
    public void onDestroy() {
        if (Param.D) Log.d(TAG, "onDestroy()");

//        mContext = null;

        super.onDestroy();
    }

    private void setLayout() {
//        // タイトルバー変更
//        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_main);
//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.activity_main_title);

        // タイトル名をランチャー名からアプリ名に変更
        setTitle(R.string.app_name);

        // フォントサイズのpx/dpを統一するため一律java側で設定する
        float fontSizeLarge = getResources().getDimension(R.dimen.fontLarge);
        TextView inputTitleView = (TextView)findViewById(R.id.inputTitleTextView);
        inputTitleView.setTextSize(fontSizeLarge);

        // スピナーの選択し選択完了時に分析開始させるためのリスナー登録
        Spinner spinnerDurationCall = (Spinner) findViewById(R.id.spinnerDurationCall);
        spinnerDurationCall.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (Param.D) Log.d(TAG, "spinnerDurationCall onItemSelected() position:" + position + " id:" + id);
                if (mDoAnalyze) {
                    // 分析開始
                    startAnalyze();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (Param.D) Log.d(TAG, "spinnerDurationCall onNothingSelected()");
            }
        });

        Spinner spinnerMonthAgo = (Spinner) findViewById(R.id.spinnerMonthAgo);
        spinnerMonthAgo.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (Param.D) Log.d(TAG, "spinnerMonthAgo onItemSelected() position:" + position + " id:" + id);
                if (mDoAnalyze) {
                    // 分析開始
                    startAnalyze();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (Param.D) Log.d(TAG, "spinnerMonthAgo onNothingSelected()");
            }
        });

        // 料金変更確定時に分析開始させるためのリスナー登録
        EditText priceEditText = (EditText)findViewById(R.id.editPrice);
        priceEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (Param.D) Log.d(TAG, "onEditorAction() actionId:" + actionId);
                // 確定ボタン選択
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // ソフトキーボードを閉じる
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    if (mDoAnalyze) {
                        // 分析開始
                        startAnalyze();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_start) {
            // 分析開始
            startAnalyze();
        }
        return true;
    }

//    public void onClickStartAnalysisButton(View view) {
//        if (Param.D) Log.d(TAG, "onClickStartAnalysisButton()");
//
//        // 分析開始
//        startAnalyze();
//    }

    private void startAnalyze() {
        // インプットデータ取得
        Spinner durationCallSpinner = (Spinner) findViewById(R.id.spinnerDurationCall);
        String item = (String) durationCallSpinner.getSelectedItem();
        int durationCall = Integer.parseInt(item.replaceAll("[^0-9]", ""));

        EditText editPriceText = (EditText) findViewById(R.id.editPrice);
        int priceCall = Integer.parseInt(editPriceText.getText().toString());

        Spinner monthSpinner = (Spinner) findViewById(R.id.spinnerMonthAgo);
        item = (String) monthSpinner.getSelectedItem();
        // 指定月数プラス当月分のデータを解析したいので１足す
        int month = Integer.parseInt(item.replaceAll("[^0-9]", "")) + 1;

        // プログレス表示
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.progress_analyze));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();

        // 分析スレッド作成
        AnalyzeThread thread = new AnalyzeThread(this, progressDialog, mMainHandler);
        thread.setMonth(month);
        thread.setCallPrice(durationCall, priceCall);
        // 分析スレッド実行
        thread.start();
    }

    public class AnalyzeThread extends Thread {
        private static final String TAG = "AnalyzeThread";
        private static final int SEC_5m = 5 * 60;
        private static final int SEC_10m = 10 * 60;

        Context mContext = null;
        ProgressDialog mProgressDialog = null;
        Handler mMainHandler = null;
        int mMonth = 4;
        int mDurationCall = 30;
        int mPriceCall = 20;

        public AnalyzeThread(Context context, ProgressDialog progressDialog, Handler handler) {
            mContext = context;
            mProgressDialog = progressDialog;
            mMainHandler = handler;
        }

        public void setMonth(int month) {
            mMonth = month;
        }

        public void setCallPrice(int duration, int price) {
            mDurationCall = duration;
            mPriceCall = price;
        }

        public void run() {
            String[] projection = new String[]{CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION};
            String selection = CallLog.Calls.TYPE + " = " + CallLog.Calls.OUTGOING_TYPE;
            String[] selectionArgs = null;
            String order = CallLog.Calls.DATE + " desc";

            Cursor cursor = null;
            // 通話ログパーミッションの許可確認
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                if (Param.D) Log.d(TAG, "run() don't enable permission !");

                // すでに拒否済かどうか
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity)mContext, Manifest.permission.READ_CALL_LOG)) {
//                    // 設定から許可しないとアプリが使えない旨を通知
//                    mMainHandler.sendEmptyMessage(Param.MSG_REQ_PERMISSION);
                    // 許可要求ダイアログ表示（一度拒否されていても再度確認）
                    ActivityCompat.requestPermissions((Activity)mContext, new String[]{Manifest.permission.READ_CALL_LOG}, 0);
                } else {
                    // 許可要求ダイアログ表示
                    ActivityCompat.requestPermissions((Activity)mContext, new String[]{Manifest.permission.READ_CALL_LOG}, 0);
                }

                mProgressDialog.dismiss();

                mProgressDialog = null;
                mMainHandler = null;
                mContext = null;

                return;
            }

            try {
                // プログレス回転表示用に少し遊び時間を持たせる
                sleep(500);
            } catch (InterruptedException e) {
            }

            // ハンドラへ投げるメッセージ生成
            Message msg = new Message();
            msg.what = Param.MSG_ANALYZE_END;
            msg.arg1 = Param.NG;

            // 通話履歴取得
            cursor = mContext.getContentResolver().query(CallLog.Calls.CONTENT_URI, projection, selection, selectionArgs, order);

            if (cursor != null && cursor.moveToFirst()) {
                if (Param.D) Log.d(TAG, "run() get cursor count:" + cursor.getCount());

                Calendar baseCalendar = Calendar.getInstance();
                baseCalendar.setTimeInMillis(System.currentTimeMillis());
                // 基準となる当月の1日にカレンダーを設定
                baseCalendar.set(baseCalendar.get(Calendar.YEAR), baseCalendar.get(Calendar.MONTH), 1);

                Calendar targetCalendar = Calendar.getInstance();

                int[][] resultData = new int[mMonth][Param.RESULT_MAX]; // 結果格納用配列
                int monthCnt = 0;   // 月
                int priceMagforNoFree = 0;   // 料金倍数(かけ放題未加入時用)
                int priceMagfor5mFree = 0;   // 料金倍数(5分かけ放題加入時用)
                int priceMagfor10mFree = 0;   // 料金倍数(10分かけ放題加入時用)

                String type = null; // 発信 or 着信
                Date date = null; // 日時
                int duration = 0; // 通話時間

                boolean lastMonth = false;

                do {
                    type = cursor.getString(cursor.getColumnIndex(CallLog.Calls.TYPE));
                    date = new Date(cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE)));
                    duration = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DURATION));

                    if (Param.D) Log.d(TAG, "type:" + type + " duration:" + duration);
                    if (Param.D) Log.d(TAG, "date:" + date);

                    targetCalendar.setTime(date);
                    // 現カーソルの対象月が基準月より前かどうか判断
                    while (targetCalendar.before(baseCalendar)) {
                        // かけ放題未加入時、5分かけ放題加入時、10分かけ放題加入時の超過料金をそれぞれ計算
                        resultData[monthCnt][Param.RESULT_NO_FREE_PLAN_PRICE] = priceMagforNoFree * mPriceCall;
                        resultData[monthCnt][Param.RESULT_5m_PLAN_REMAIN_PRICE] = priceMagfor5mFree * mPriceCall;
                        resultData[monthCnt][Param.RESULT_10m_PLAN_REMAIN_PRICE] = priceMagfor10mFree * mPriceCall;

                        // 料金倍数計算用パラメータを初期化
                        priceMagforNoFree = 0;
                        priceMagfor5mFree = 0;
                        priceMagfor10mFree = 0;

                        if (Param.D) Log.d(TAG, "monthCnt:" + monthCnt + " outgoing:" + resultData[monthCnt][Param.RESULT_OUTGOING_CNT] + " within_5ms:" + resultData[monthCnt][Param.RESULT_WITHIN_5m_CNT] + " 5to10ms:" + resultData[monthCnt][Param.RESULT_WITHIN_10m_CNT] + " over10ms:" + resultData[monthCnt][Param.RESULT_OVER_10m_CNT]);
                        if (Param.D) Log.d(TAG, "noFreePrice:" + resultData[monthCnt][Param.RESULT_NO_FREE_PLAN_PRICE] + " 5msRemainPrice:" + resultData[monthCnt][Param.RESULT_5m_PLAN_REMAIN_PRICE] + " 10msRemainPrice:" + resultData[monthCnt][Param.RESULT_10m_PLAN_REMAIN_PRICE]);

                        // 月を進める
                        monthCnt++;

                        // 最後の月か判定
                        if (monthCnt < mMonth) {
                            // 基準月を1か月前に戻す
                            baseCalendar.add(Calendar.MONTH, -1);
                        } else {
                            // 最後の月の計算完了と判断しループを抜ける
                            lastMonth = true;
                            break;
                        }
                    }

                    if (!lastMonth) {
                        // 発信回数をカウント
                        resultData[monthCnt][Param.RESULT_OUTGOING_CNT]++;
                        priceMagforNoFree += ((duration / mDurationCall) + 1);

                        // 5分以内、5～10分以内、10分超過を判別しそれぞれカウント
                        if (duration <= SEC_5m) {
                            resultData[monthCnt][Param.RESULT_WITHIN_5m_CNT]++;
                        } else if (duration <= SEC_10m) {
                            resultData[monthCnt][Param.RESULT_WITHIN_10m_CNT]++;
                            // 5分かけ放題の場合の超過料金分を加算
                            priceMagfor5mFree += (((duration - SEC_5m) / mDurationCall) + 1);
                        } else {
                            resultData[monthCnt][Param.RESULT_OVER_10m_CNT]++;
                            // 5分かけ放題の場合の超過料金分を加算
                            priceMagfor5mFree += (((duration - SEC_5m) / mDurationCall) + 1);
                            // 10分かけ放題の場合の超過料金分を加算
                            priceMagfor10mFree += (((duration - SEC_10m) / mDurationCall) + 1);
                        }
                    }

                } while(!lastMonth && cursor.moveToNext());

                // 最終月判定よりカーソル終端に先に到達してしまった場合は最後の月の計算を実施
                if (!lastMonth) {
                    // かけ放題未加入時、5分かけ放題加入時、10分かけ放題加入時の超過料金をそれぞれ計算
                    resultData[monthCnt][Param.RESULT_NO_FREE_PLAN_PRICE] = priceMagforNoFree * mPriceCall;
                    resultData[monthCnt][Param.RESULT_5m_PLAN_REMAIN_PRICE] = priceMagfor5mFree * mPriceCall;
                    resultData[monthCnt][Param.RESULT_10m_PLAN_REMAIN_PRICE] = priceMagfor10mFree * mPriceCall;

                    if (Param.D) Log.d(TAG, "monthCnt:" + monthCnt + " outgoing:" + resultData[monthCnt][Param.RESULT_OUTGOING_CNT] + " within_5ms:" + resultData[monthCnt][Param.RESULT_WITHIN_5m_CNT] + " 5to10ms:" + resultData[monthCnt][Param.RESULT_WITHIN_10m_CNT] + " over10ms:" + resultData[monthCnt][Param.RESULT_OVER_10m_CNT]);
                    if (Param.D) Log.d(TAG, "noFreePrice:" + resultData[monthCnt][Param.RESULT_NO_FREE_PLAN_PRICE] + " 5msRemainPrice:" + resultData[monthCnt][Param.RESULT_5m_PLAN_REMAIN_PRICE] + " 10msRemainPrice:" + resultData[monthCnt][Param.RESULT_10m_PLAN_REMAIN_PRICE]);
                }

                cursor.close();

                // 結果をメッセージにセット
                msg.obj = (Object)resultData;
                msg.arg1 = Param.OK;
            }

            // ハンドラメッセージを投げる
            mMainHandler.sendMessage(msg);

            mProgressDialog.dismiss();

            mProgressDialog = null;
            mMainHandler = null;
            mContext = null;
        }
    }

    Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (Param.D) Log.d(TAG, "handleMessage() what:" + msg.what + " arg1:" + msg.arg1);
            switch (msg.what) {
                case Param.MSG_ANALYZE_END:
                    if (Param.OK == msg.arg1) {
                        // 分析成功
                        addAnalyzeResultLayout((int[][])msg.obj);
                        mDoAnalyze = true;
                    } else {
                        // 分析失敗
                        Toast.makeText(getApplicationContext(), R.string.toast_analyze_error, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Param.MSG_REQ_PERMISSION:
                    // パーミッション許可のお願い
                    Toast.makeText(getApplicationContext(), R.string.toast_request_permission_call_log, Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    };

    private final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
    private final int MP = ViewGroup.LayoutParams.MATCH_PARENT;

    private void addAnalyzeResultLayout(int[][] resultData) {
        LinearLayout baseLayout = (LinearLayout)findViewById(R.id.layoutAnalyzeResult);

        // 一旦空にする
        baseLayout.removeAllViews();

        LinearLayout titleLayout = null;
        LinearLayout resultLayout = null;

        // 毎月固定の文字列定義
        TextView callCntTitleView = null;

        TextView callOutCntView = null;
        TextView within5mCntView = null;
        TextView within10mCntView = null;
        TextView over10mCntView = null;

        TextView priceTitleView = null;

        TextView noFreePlanPriceView = null;
        TextView within5mPriceView = null;
        TextView within10mPriceView = null;
        TextView goodValue5mView = null;
        TextView goodValue10mView = null;

        // 年月表示
        TextView yymmView = null;
        TextView spaceView = null;

        Calendar baseCalendar = Calendar.getInstance();
        baseCalendar.setTimeInMillis(System.currentTimeMillis());
        // 基準となる当月の1日にカレンダーを設定
        baseCalendar.set(baseCalendar.get(Calendar.YEAR), baseCalendar.get(Calendar.MONTH), 1);

        SimpleDateFormat sdf = null;
        if (Locale.getDefault().getLanguage().equals("ja")) {
            // 日本語表記
            sdf = new SimpleDateFormat("yyyy年MM月");
        } else {
            // 英語表記
            sdf = new SimpleDateFormat("yyyy/MM");
        }

        int size = resultData.length;
        String yymm = null;

        int textPadding = getResources().getDimensionPixelSize(R.dimen.textPadding);
        float fontSizeHalfLarge = getResources().getDimension(R.dimen.fontHalfLarge);

        for (int i = 0; i < size; i++) {
            // レイアウトの設定

            // 年月セット
            titleLayout = new LinearLayout(this);
            titleLayout.setOrientation(LinearLayout.VERTICAL);
            //titleLayout.setBackgroundResource(R.color.colorTitle2);
            titleLayout.setBackground(getBorderDrawable());

            yymmView = new TextView(this);
            yymm = sdf.format(baseCalendar.getTime());
            if (i == 0) yymm += getString(R.string.text_title_this_month);
            if (Param.D) Log.d(TAG, "addAnalyzeResultLayout() size:" + size + " i:" + i + " yymm:" + yymm);
            yymmView.setText(yymm);
            yymmView.setPadding(textPadding, textPadding, 0, textPadding); // left, top, right, bottom
            yymmView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFontLight));
            yymmView.setTextSize(fontSizeHalfLarge);
            yymmView.setTypeface(Typeface.DEFAULT_BOLD);

//            baseLayout.addView(yymmView, new LinearLayout.LayoutParams(MP, WC));
            titleLayout.addView(yymmView, new LinearLayout.LayoutParams(MP, WC));
            baseLayout.addView(titleLayout, new LinearLayout.LayoutParams(MP, WC));


            resultLayout = new LinearLayout(this);
            resultLayout.setOrientation(LinearLayout.VERTICAL);
            resultLayout.setBackgroundResource(R.color.colorBase2);

            // 発信が1件以上あった場合
            if (resultData[i][Param.RESULT_OUTGOING_CNT] > 0) {
//                // スペースをあける
//                spaceView = new TextView(this);
//                spaceView.setText("");
//                baseLayout.addView(spaceView, new LinearLayout.LayoutParams(MP, WC));

                // 【発信回数】セット
                callCntTitleView = new TextView(this);
                callCntTitleView.setText(R.string.text_result_title_call_out_cnt);
                callCntTitleView.setTypeface(Typeface.DEFAULT_BOLD);

//                baseLayout.addView(callCntTitleView, new LinearLayout.LayoutParams(MP, WC));
                resultLayout.addView(callCntTitleView, new LinearLayout.LayoutParams(MP, WC));

                callOutCntView = new TextView(this);
                within5mCntView = new TextView(this);
                within10mCntView = new TextView(this);
                over10mCntView = new TextView(this);

                callOutCntView.setText(getString(R.string.text_result_total, resultData[i][Param.RESULT_OUTGOING_CNT]));
                within5mCntView.setText(getString(R.string.text_result_within_5m, resultData[i][Param.RESULT_WITHIN_5m_CNT]));
                within10mCntView.setText(getString(R.string.text_result_within_5m_to_10m, resultData[i][Param.RESULT_WITHIN_10m_CNT]));
                over10mCntView.setText(getString(R.string.text_result_over_10m, resultData[i][Param.RESULT_OVER_10m_CNT]));

//                baseLayout.addView(callOutCntView, new LinearLayout.LayoutParams(MP, WC));
//                baseLayout.addView(within5mCntView, new LinearLayout.LayoutParams(MP, WC));
//                baseLayout.addView(within10mCntView, new LinearLayout.LayoutParams(MP, WC));
//                baseLayout.addView(over10mCntView, new LinearLayout.LayoutParams(MP, WC));
                resultLayout.addView(callOutCntView, new LinearLayout.LayoutParams(MP, WC));
                resultLayout.addView(within5mCntView, new LinearLayout.LayoutParams(MP, WC));
                resultLayout.addView(within10mCntView, new LinearLayout.LayoutParams(MP, WC));
                resultLayout.addView(over10mCntView, new LinearLayout.LayoutParams(MP, WC));

                // 【料金シミュレーション】セット
                priceTitleView = new TextView(this);
                priceTitleView.setText(R.string.text_result_title_price_image);
                priceTitleView.setTypeface(Typeface.DEFAULT_BOLD);
                priceTitleView.setPadding(0, textPadding, 0, 0);

//                baseLayout.addView(priceTitleView, new LinearLayout.LayoutParams(MP, WC));
                resultLayout.addView(priceTitleView, new LinearLayout.LayoutParams(MP, WC));

                noFreePlanPriceView = new TextView(this);
                within5mPriceView = new TextView(this);
                goodValue5mView = new TextView(this);
                within10mPriceView = new TextView(this);
                goodValue10mView = new TextView(this);

                noFreePlanPriceView.setText(getString(R.string.text_analysis_no_free_plan, resultData[i][Param.RESULT_NO_FREE_PLAN_PRICE]));
                within5mPriceView.setText(getString(R.string.text_analysis_5m_free_plan, resultData[i][Param.RESULT_5m_PLAN_REMAIN_PRICE]));
                goodValue5mView.setText(getString(R.string.text_analysis_5m_good_value, (resultData[i][Param.RESULT_NO_FREE_PLAN_PRICE] - resultData[i][Param.RESULT_5m_PLAN_REMAIN_PRICE])));
                goodValue5mView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFontForce));
                goodValue5mView.setTypeface(Typeface.DEFAULT_BOLD);
                within10mPriceView.setText(getString(R.string.text_analysis_10m_free_plan, resultData[i][Param.RESULT_10m_PLAN_REMAIN_PRICE]));
                goodValue10mView.setText(getString(R.string.text_analysis_10m_good_value, (resultData[i][Param.RESULT_NO_FREE_PLAN_PRICE] - resultData[i][Param.RESULT_10m_PLAN_REMAIN_PRICE])));
                goodValue10mView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorFontForce));
                goodValue10mView.setTypeface(Typeface.DEFAULT_BOLD);

//                baseLayout.addView(noFreePlanPriceView, new LinearLayout.LayoutParams(MP, WC));
//                baseLayout.addView(within5mPriceView, new LinearLayout.LayoutParams(MP, WC));
//                baseLayout.addView(within10mPriceView, new LinearLayout.LayoutParams(MP, WC));
                resultLayout.addView(noFreePlanPriceView, new LinearLayout.LayoutParams(MP, WC));
                resultLayout.addView(within5mPriceView, new LinearLayout.LayoutParams(MP, WC));
                resultLayout.addView(goodValue5mView, new LinearLayout.LayoutParams(MP, WC));
                resultLayout.addView(within10mPriceView, new LinearLayout.LayoutParams(MP, WC));
                resultLayout.addView(goodValue10mView, new LinearLayout.LayoutParams(MP, WC));
            } else {
                // 発信が1件もなかった場合
                callCntTitleView = new TextView(this);
                callCntTitleView.setText(R.string.text_result_no_call_out_cnt);

//                baseLayout.addView(callCntTitleView, new LinearLayout.LayoutParams(MP, WC));
                resultLayout.addView(callCntTitleView, new LinearLayout.LayoutParams(MP, WC));
            }
            resultLayout.setPadding(textPadding, textPadding, 0 , textPadding);

            baseLayout.addView(resultLayout, new LinearLayout.LayoutParams(MP, WC));

            // スペースをあける
            spaceView = new TextView(this);
            spaceView.setText("");
            baseLayout.addView(spaceView, new LinearLayout.LayoutParams(MP, WC));

            // 年月を1戻す
            baseCalendar.add(Calendar.MONTH, -1);
        }
    }
    private LayerDrawable getBorderDrawable() {
        GradientDrawable borderDrawable = new GradientDrawable();
        borderDrawable.setColor(ContextCompat.getColor(getApplicationContext(), R.color.colorTitle2));
        borderDrawable.setStroke(getResources().getDimensionPixelSize(R.dimen.strokeWidth), ContextCompat.getColor(getApplicationContext(), R.color.colorFontLight));

        // LayerDrawableにボーダーを付けたDrawableをセット
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{borderDrawable});

        return layerDrawable;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: { //ActivityCompat#requestPermissions()の第2引数で指定した値
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //許可された場合の処理

                    // 改めて分析開始
                    startAnalyze();
                }else{
                    //拒否された場合の処理

                    // パーミッション許可のお願い
                    Toast.makeText(getApplicationContext(), R.string.toast_request_permission_call_log, Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    @Override
    public void onBackPressed(){
        // レビュー済か否か
        if (!PrefUtils.readPrefBool(this, PrefUtils.KEY_SETTING_REVIEW_DONE)) {
            Random r = new Random();
            // 3分の1の確立でダイアログを表示
            if (r.nextInt(3) == 0) {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.dialog_title_review))
                        .setMessage(getString(R.string.msg_request_review))
                        .setPositiveButton(getString(R.string.dialog_select_ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // レビューするを選択
                                PrefUtils.writePrefBool(getApplicationContext(), PrefUtils.KEY_SETTING_REVIEW_DONE, true);

                                Intent googlePlayIntent = new Intent(Intent.ACTION_VIEW);
                                googlePlayIntent.setData(Uri.parse("market://details?id=jp.anmt.phoneplanchecker"));
                                startActivity(googlePlayIntent);
                            }
                        })
                        .setNegativeButton(getString(R.string.dialog_select_ng), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // レビューしないを選択
                                finish();
                            }
                        })
                        .show();

                return;
            }
        }

        super.onBackPressed();
    }
}

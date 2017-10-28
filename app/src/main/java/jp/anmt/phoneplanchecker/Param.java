package jp.anmt.phoneplanchecker;

/**
 * Created by numata on 2017/08/18.
 */

public class Param {
    // デバッグモード有効
    public static final boolean D = false;

    public static final int OK = 1;
    public static final int NG = 0;

    public static final int RESULT_OUTGOING_CNT = 0;
    public static final int RESULT_WITHIN_5m_CNT = 1;
    public static final int RESULT_WITHIN_10m_CNT = 2;
    public static final int RESULT_OVER_10m_CNT = 3;
    public static final int RESULT_NO_FREE_PLAN_PRICE = 4;
    public static final int RESULT_5m_PLAN_REMAIN_PRICE = 5;
    public static final int RESULT_10m_PLAN_REMAIN_PRICE = 6;
    public static final int RESULT_MAX = 7;

    public static final int MSG_ANALYZE_END = 0;
    public static final int MSG_REQ_PERMISSION = 1;

}

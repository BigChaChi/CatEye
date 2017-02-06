package com.cicinnus.cateye.module.movie.find_movie.fixedboard_movie.oversea_movie;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cicinnus.cateye.R;
import com.cicinnus.cateye.base.BaseFragment;
import com.cicinnus.cateye.module.movie.find_movie.fixedboard_movie.oversea_movie.bean.OverseaComingMovieBean;
import com.cicinnus.cateye.module.movie.find_movie.fixedboard_movie.oversea_movie.bean.OverseaHotMovieBean;
import com.cicinnus.cateye.net.SchedulersCompat;
import com.cicinnus.cateye.tools.UiUtils;
import com.cicinnus.cateye.view.MyPullToRefreshListener;
import com.cicinnus.cateye.view.ProgressLayout;
import com.cicinnus.cateye.view.SuperSwipeRefreshLayout;
import com.google.gson.Gson;
import com.orhanobut.logger.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import okhttp3.ResponseBody;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * Created by Cicinnus on 2017/2/5.
 */

public class OverseaMovieFragment extends BaseFragment<OverseaMoviePresenter> implements OverseaMovieContract.IOverseaMovieView {

    private static final String AREA = "area";
    private String area;
    private MyPullToRefreshListener pullToRefreshListener;
    private Gson gson;

    public static OverseaMovieFragment newInstance(String area) {

        Bundle args = new Bundle();
        args.putString(AREA, area);
        OverseaMovieFragment fragment = new OverseaMovieFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @BindView(R.id.swipe)
    SuperSwipeRefreshLayout swipe;
    @BindView(R.id.progressLayout)
    ProgressLayout progressLayout;
    @BindView(R.id.rv_oversea_movie)
    RecyclerView rvOverseaMovie;

    private OverseaMovieAdapter overseaMovieAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_oversea_movie;
    }

    @Override
    protected OverseaMoviePresenter getPresenter() {
        return new OverseaMoviePresenter(mContext, this);
    }

    @Override
    protected void initEventAndData() {
        area = getArguments().getString(AREA);
        gson = new Gson();


        overseaMovieAdapter = new OverseaMovieAdapter();
        rvOverseaMovie.setLayoutManager(new LinearLayoutManager(mContext));
        rvOverseaMovie.setAdapter(overseaMovieAdapter);

        pullToRefreshListener = new MyPullToRefreshListener(mContext, swipe);
        swipe.setOnPullRefreshListener(pullToRefreshListener);
        pullToRefreshListener.setOnRefreshListener(new MyPullToRefreshListener.OnRefreshListener() {
            @Override
            public void refresh() {
                mPresenter.getOverseaMovie(area);
            }
        });
    }

    @Override
    protected void lazyLoadEveryTime() {
        mPresenter.getOverseaMovie(area);
    }


    @Override
    public void showLoading() {
        if (!progressLayout.isContent()) {
            progressLayout.showLoading();
        }
    }

    @Override
    public void showContent() {
        if (!progressLayout.isContent()) {
            progressLayout.showContent();
        }
    }

    @Override
    public void showError(String errorMsg) {

        progressLayout.showError(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPresenter.getOverseaMovie(area);
            }
        });
    }

    @Override
    public void addOverseaMovie(ResponseBody responseBody) {
        try {
            JSONObject jsonObject = new JSONObject(responseBody.string());
            JSONObject data = (JSONObject) jsonObject.get("data");
            JSONObject hotData = (JSONObject) data.get("http://api.maoyan.com/mmdb/movie/oversea/hot.json?area=" + area + "&offset=0&limit=10");
            JSONObject hot = (JSONObject) hotData.get("data");
            OverseaHotMovieBean.DataBean hotDataBean = gson.fromJson(hot.toString(), OverseaHotMovieBean.DataBean.class);
            overseaMovieAdapter.setNewData(hotDataBean.getHot());
            TextView headerView = new TextView(mContext);
            int padding = UiUtils.dp2px(mContext,10);
            headerView.setPadding(padding,padding,0,padding);
            String title = "";
            switch (area) {
                case "NA":
                    title = "美国热映";
                    break;
                case "KR":
                    title = "韩国热映";
                    break;
                case "JP":
                    title = "日本热映";
                    break;
            }
            headerView.setText(title);
            overseaMovieAdapter.addHeaderView(headerView);
            View footerView = mContext.getLayoutInflater().inflate(R.layout.layout_oversea_footer, (ViewGroup) rvOverseaMovie.getParent(), false);
            ((TextView) footerView.findViewById(R.id.tv_oversea_footer)).setText("查看全部热门电影");
            overseaMovieAdapter.addFooterView(footerView);


            JSONObject comingList = (JSONObject) data.get("http://api.maoyan.com/mmdb/movie/oversea/coming.json?area=" + area + "&offset=0&limit=10");
            final JSONObject coming = (JSONObject) comingList.get("data");
            OverseaComingMovieBean.DataBean comingData = gson.fromJson(coming.toString(), OverseaComingMovieBean.DataBean.class);
            Observable.from(comingData.getComing())
                    .map(new Func1<OverseaComingMovieBean.DataBean.ComingBean, OverseaHotMovieBean.DataBean.HotBean>() {
                        @Override
                        public OverseaHotMovieBean.DataBean.HotBean call(OverseaComingMovieBean.DataBean.ComingBean comingBean) {
                            OverseaHotMovieBean.DataBean.HotBean hotBean = new OverseaHotMovieBean.DataBean.HotBean();
                            List<OverseaHotMovieBean.DataBean.HotBean.HeadLinesVOBean> headLineList = new ArrayList<>();
                            if(comingBean.getHeadLinesVO()!=null) {
                                for (int i = 0; i < comingBean.getHeadLinesVO().size(); i++) {
                                    OverseaHotMovieBean.DataBean.HotBean.HeadLinesVOBean headLinesVOBean = new OverseaHotMovieBean.DataBean.HotBean.HeadLinesVOBean();
                                    headLinesVOBean.setMovieId(comingBean.getHeadLinesVO().get(i).getMovieId());
                                    headLinesVOBean.setTitle(comingBean.getHeadLinesVO().get(i).getTitle());
                                    headLinesVOBean.setType(comingBean.getHeadLinesVO().get(i).getType());
                                    headLinesVOBean.setUrl(comingBean.getHeadLinesVO().get(i).getUrl());
                                    headLineList.add(headLinesVOBean);
                                }
                            }
                            hotBean.setHeadLinesVO(headLineList);
                            hotBean.setStar(comingBean.getStar());
                            hotBean.setShowst(comingBean.getShowst());
                            hotBean.setWish(comingBean.getWish());
                            hotBean.setVideourl(comingBean.getVideourl());
                            hotBean.setVideoName(comingBean.getVideoName());
                            hotBean.setStar(comingBean.getStar());
                            hotBean.setNm(comingBean.getNm());
                            return hotBean;
                        }
                    })
                    .toList()
                    .compose(SchedulersCompat.<List<OverseaHotMovieBean.DataBean.HotBean>>applyIoSchedulers())
                    .subscribe(new Subscriber<List<OverseaHotMovieBean.DataBean.HotBean>>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            Logger.e(e.getMessage());
                        }

                        @Override
                        public void onNext(List<OverseaHotMovieBean.DataBean.HotBean> hotBeen) {

                            TextView headerView = new TextView(mContext);
                            int padding = UiUtils.dp2px(mContext,10);
                            headerView.setPadding(padding,padding,0,padding);
                            String title = "";
                            switch (area) {
                                case "NA":
                                    title = "美国热映";
                                    break;
                                case "KR":
                                    title = "韩国热映";
                                    break;
                                case "JP":
                                    title = "日本热映";
                                    break;
                            }
                            headerView.setText(title);
                            overseaMovieAdapter.addData(hotBeen);
                            View footerView = mContext.getLayoutInflater().inflate(R.layout.layout_oversea_footer, (ViewGroup) rvOverseaMovie.getParent(), false);
                            ((TextView) footerView.findViewById(R.id.tv_oversea_footer)).setText("查看全部待映电影");
                            overseaMovieAdapter.addFooterView(footerView);
                        }
                    });

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }
}

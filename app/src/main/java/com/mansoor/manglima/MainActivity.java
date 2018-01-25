package com.mansoor.manglima;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;

import com.bhargavms.dotloader.DotLoader;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.common.base.Supplier;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;

import cn.pedant.SweetAlert.SweetAlertDialog;


public class MainActivity extends AppCompatActivity {

    private DBHelper mydb;
    private EditText word;
    private TextView tvResult,app_link_text,textHistorySummary;
    private Set<String> keys;
    private List<String> autoCompleteList = null, resultList = null, resultList2 = null,resultList3=null;
    private long delay = 500; // milliseconds after user stops typing
    private long last_text_edit = 0;
    private Handler handler = new Handler();
    private CharSequence searchText;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private ListView autocompleteListView, searchResultListView, historyListView;
    private ArrayAdapter<String> englishAutoCompleteAdapter, englishResultAdapter, historyAdapter;
    private String str, str2 = "";
    private int n = 0;
    private Collection<String> values;
    private TextToSpeech tts;
    private LinearLayout linearResult, layout_em,layout_history;
    private RelativeLayout info;
    private BottomNavigationView bottom_navigation_view;
    private HashMap<String, String> wordTypes = new HashMap<String, String>();
    private ListMultimap<String, String> multi_map = Multimaps.newListMultimap(
            new TreeMap<String, Collection<String>>(),
            new Supplier<List<String>>() {
                public List<String> get() {
                    return Lists.newArrayList();
                }
            });
    private DotLoader dotLoader;
    private RelativeLayout loader;
    private boolean englishPage = true,showAds=true;
    private InterstitialAd mInterstitialAd;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    loadEnglishToMalayalam(item);
                    return true;
                case R.id.navigation_dashboard:
                    loadMalayalamToEnshish(item);
                    return true;
                case R.id.navigation_notifications:
                    loadHistory();
                    return true;
            }
            return false;
        }

    };


    private void loadEnglishToMalayalam(MenuItem item) {
        if (mInterstitialAd.isLoaded()&& showAds) {
            showInterAds();
        }
        setTitle(R.string.title_em);
        englishPage = true;
        layout_em.setVisibility(View.VISIBLE);
        layout_history.setVisibility(View.GONE);
        info.setVisibility(View.GONE);
        word.setHint(R.string.et_word);
        clearSearch(item.getActionView());
    }

    private void loadMalayalamToEnshish(MenuItem item) {
        if (mInterstitialAd.isLoaded()&& showAds) {
            showInterAds();
        }
        setTitle(R.string.title_me);
        englishPage = false;
        layout_em.setVisibility(View.VISIBLE);
        layout_history.setVisibility(View.GONE);
        word.setHint(R.string.et_ml_word);
        clearSearch(item.getActionView());
        info.setVisibility(View.VISIBLE);
    }


    private void loadHistory() {
        if (mInterstitialAd.isLoaded()&& showAds) {
            showInterAds();
        }
        setTitle(R.string.title_library);
        layout_em.setVisibility(View.GONE);
        info.setVisibility(View.GONE);
        new ShowHistory().execute();

    }

    private void showInterAds() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mInterstitialAd.show();
                showAds = false;
            }
        }, 10000);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        new GenerateDictionary().execute();

        // Interstitial ads
        MobileAds.initialize(this, "ca-app-pub-1243068719441957~6247454596");// Production
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-3940256099942544/1033173712"); // Testing
//        mInterstitialAd.setAdUnitId("ca-app-pub-1243068719441957/7471415668"); // Production
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        inializeVariables();

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.ENGLISH);
                }
            }
        });

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });

        word.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                if (englishPage) {
                    if (s.length() > 0) {
                        searchText = s;
                        last_text_edit = System.currentTimeMillis();
                        handler.postDelayed(input_finish_checker, delay);
                        searchResultListView.setVisibility(View.GONE);
                        linearResult.setVisibility(View.GONE);
                        autocompleteListView.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (s.length() > 2) {
                        searchText = s;
                        last_text_edit = System.currentTimeMillis();
                        handler.postDelayed(input_finish_checker2, delay);
                        searchResultListView.setVisibility(View.GONE);
                        linearResult.setVisibility(View.GONE);
                        autocompleteListView.setVisibility(View.VISIBLE);
                    }
                }
            }

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(final CharSequence s, int start,
                                      int before, int count) {
                //You need to remove this to run only once
                handler.removeCallbacks(input_finish_checker);
                if (count != 0) {
                    if (englishPage)
                        word.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clear_white_24dp, 0, R.drawable.ic_settings_voice_white_24dp, 0);
                    else {
                        word.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clear_white_24dp, 0, 0, 0);
                        info.setVisibility(View.GONE);
                    }
                }
                else {
                    if (englishPage)
                        word.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search_white_24dp, 0, R.drawable.ic_settings_voice_white_24dp, 0);
                    else
                        word.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search_white_24dp, 0, 0, 0);
                }
            }
        });
        word.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_LEFT = 0;
                final int DRAWABLE_TOP = 1;
                final int DRAWABLE_RIGHT = 2;
                final int DRAWABLE_BOTTOM = 3;
                try {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        if (event.getRawX() >= (word.getRight() - word.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                            promptSpeechInput();
                            return true;
                        } else if (event.getRawX() >= (word.getLeft() - word.getCompoundDrawables()[DRAWABLE_LEFT].getBounds().width())) {
                            word.setText("");
                            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                            return true;
                        }
                    }
                }catch (NullPointerException n){
                    word.setText("");
                    word.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search_white_24dp, 0, 0, 0);
                }
                return false;
            }
        });

        autocompleteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                findOutMeaning(autoCompleteList.get(position));
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });
        searchResultListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(!englishPage){
                    tts.speak(resultList2.get(position).split(" - ")[0], TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });
        historyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                new SweetAlertDialog(MainActivity.this)
                        .setTitleText(resultList2.get(position))
                        .setContentText(resultList3.get(position))
                        .setCancelText("Cancel")
                        .setConfirmText("Remove word")
                        .showCancelButton(true)
                        .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                            @Override
                            public void onClick(SweetAlertDialog sDialog) {
                                mydb.deleteWord(resultList2.get(position));
                                new ShowHistory().execute();
                                sDialog.dismissWithAnimation();
                            }
                        })
                        .show();
            }
        });
    }

    private void findOutMeaning(String s) {
        str = s;
        word.setText(str);
        if(englishPage) {
            resultList = multi_map.get(str);
            resultList2.clear();
            Collections.sort(resultList, Collections.reverseOrder());
            for (int i = 0; i < resultList.size(); i++) {

                str = resultList.get(i).split(" - ")[0];
                if (!str2.equals(str))
                    resultList2.add(wordTypes.get(str));
                str2 = str;
                resultList2.add(" •  " + resultList.get(i).split(" - ")[1]);
            }
            englishResultAdapter.clear();
            englishResultAdapter.addAll(resultList2);
            englishResultAdapter.notifyDataSetChanged();
            autocompleteListView.setVisibility(View.GONE);
            searchResultListView.setVisibility(View.VISIBLE);
            linearResult.setVisibility(View.VISIBLE);
            tvResult.setText(resultList.size() + " result(s)");
            mydb.insertHistory(word.getText().toString(),resultList.toString());

        }else{
            new ShowMalayalamResult().execute();
        }
    }

    private void inializeVariables() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.drawable.ic_search_white_24dp);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        setTitle(R.string.app_name);
        word = (EditText) findViewById(R.id.word);
        autocompleteListView = (ListView) findViewById(R.id.autocompleteList);
        searchResultListView = (ListView) findViewById(R.id.searchResultteList);
        historyListView = (ListView) findViewById(R.id.historyList);
        linearResult = (LinearLayout) findViewById(R.id.linearResult);
        layout_em = (LinearLayout) findViewById(R.id.layout_em);
        layout_history = (LinearLayout) findViewById(R.id.layout_history);
        info = (RelativeLayout) findViewById(R.id.info);
        bottom_navigation_view = (BottomNavigationView) findViewById(R.id.navigation);
        loader = (RelativeLayout) findViewById(R.id.loader);
        dotLoader = (DotLoader) findViewById(R.id.dot_loader);
        tvResult = (TextView) findViewById(R.id.tvResult);
        app_link_text = (TextView) findViewById(R.id.app_link_text);
        textHistorySummary = (TextView) findViewById(R.id.textHistorySummary);
        app_link_text.setMovementMethod(LinkMovementMethod.getInstance());
        autoCompleteList = new ArrayList<String>();
        resultList = new ArrayList<String>();
        resultList2 = new ArrayList<String>();
        englishAutoCompleteAdapter = new ArrayAdapter<String>(this, R.layout.simplerow, autoCompleteList);
        englishResultAdapter = new ArrayAdapter<String>(this, R.layout.simplerow_result, resultList);
        historyAdapter = new ArrayAdapter<String>(this, R.layout.simplerow, resultList2);
        autocompleteListView.setAdapter(englishAutoCompleteAdapter);
        searchResultListView.setAdapter(englishResultAdapter);
        historyListView.setAdapter(historyAdapter);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        mydb = new DBHelper(MainActivity.this);
        wordTypes.put("n", "Noun");
        wordTypes.put("propn", "Noun");
        wordTypes.put("pron", "Pronoun");
        wordTypes.put("v", "Verb");
        wordTypes.put("phr", "Phrase");
        wordTypes.put("prep", "Preposition");
        wordTypes.put("conj", "Conjunction");
        wordTypes.put("idm", "Idiom");
        wordTypes.put("a", "Adjective");
        wordTypes.put("adv", "Adverb");
        wordTypes.put("-", "Other");
    }

    public void speakTheWord(View v) {
        if(englishPage)
            tts.speak(word.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
        else
            Toast.makeText(MainActivity.this, "Click on any result to hear", Toast.LENGTH_LONG).show();
    }

    public void share(View v) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/html");
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Word: " + word.getText() + "\nMeaning: " + resultList2.toString());
        startActivity(Intent.createChooser(sharingIntent, "Share using"));
    }

    public void clearSearch(View v) {
        word.setText("");
        englishResultAdapter.clear();
        englishResultAdapter.notifyDataSetChanged();
        searchResultListView.setVisibility(View.GONE);
        linearResult.setVisibility(View.GONE);
        //((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }
    public void clearHistory(View v) {
        if(resultList2.size() == 0)
            Toast.makeText(this, "Empty history", Toast.LENGTH_SHORT).show();
        else
            new SweetAlertDialog(MainActivity.this,SweetAlertDialog.WARNING_TYPE)
                    .setContentText("Confirm to clear search history?")
                    .setTitleText("Clear history")
                .setCancelText("Cancel")
                .setConfirmText("Confirm")
                .showCancelButton(true)
                .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                    @Override
                    public void onClick(SweetAlertDialog sDialog) {
                        mydb.deleteHistory();
                        new ShowHistory().execute();
                        sDialog.dismissWithAnimation();
                    }
                })
                .show();
    }


    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//                    txtSpeechInput.setText(result.get(0));
                    word.setText(result.get(0));
                }
                break;
            }

        }
    }

    private Runnable input_finish_checker = new Runnable() {
        public void run() {
            if (System.currentTimeMillis() > (last_text_edit + delay - 500)) {
                // TODO: do what you need here
                // ............
                // ............
                new AutocompleteWord().execute(searchText.toString());
            }
        }
    };
    private Runnable input_finish_checker2 = new Runnable() {
        public void run() {
            if (System.currentTimeMillis() > (last_text_edit + delay - 500)) {
                // TODO: do what you need here
                // ............
                // ............
                new MalayalamAutocompleteWord().execute(searchText.toString());
            }
        }
    };


    public class GenerateDictionary extends AsyncTask<String, String, String> {
        String response = "";

        @Override
        protected void onPreExecute() {
        }

        protected String doInBackground(String... params) {
            try {
                InputStreamReader is = null;
                try {
                    is = new InputStreamReader(getAssets()
                            .open("olam.csv"));
                    BufferedReader reader = new BufferedReader(is);
                    reader.readLine();
                    String line, phrases[];
                    while ((line = reader.readLine()) != null) {
                        try {
                            phrases = line.split("\t");
                            //mydb.insertWord(phrases[1], phrases[3], phrases[2]);
                            multi_map.put(phrases[1], phrases[2] + " - " + phrases[3]);
                            n++;
                            //publishProgress((n / 216450.0) * 100 + "");
                        } catch (Exception e) {
                        }
                    }
                    keys = multi_map.keySet();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            } catch (Exception e) {
                return response;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            loader.setVisibility(View.GONE);
            getSupportActionBar().show();
            setTitle(R.string.title_em);
            bottom_navigation_view.setVisibility(View.VISIBLE);
            layout_em.setVisibility(View.VISIBLE);
            word.requestFocus();
            AdView  mAdView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            showInterAds();
//        ("ca-app-pub-3940256099942544/6300978111");// Testing
//      ("ca-app-pub-1243068719441957/5094510241");// Production
//            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    public class AutocompleteWord extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            autoCompleteList = new ArrayList<String>();
        }

        protected String doInBackground(String... params) {
            int n = 0;
            for (String string : keys) {
                if (string.toLowerCase().startsWith(params[0].toLowerCase())) {
                    autoCompleteList.add(string);
                    n++;
                    if (n >= 5)
                        break;
                }
            }

            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            englishAutoCompleteAdapter.clear();
            englishAutoCompleteAdapter.addAll(autoCompleteList);
            englishAutoCompleteAdapter.notifyDataSetChanged();
        }
    }

    public class MalayalamAutocompleteWord extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            autoCompleteList = new ArrayList<String>();
        }

        protected String doInBackground(String... params) {
            int n = 0;
            values = multi_map.values();
            for (Object value : values) {
                if (value.toString().contains(params[0])) {
                    str = value.toString().split(" - ")[1];
                    if(str.startsWith(params[0])) {
                        autoCompleteList.add(str);
                        if(n ==0)
                            autoCompleteList = new ArrayList<String>();
                        n++;
                        if (n >= 5)
                            break;
                    }
                }
            }

            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            englishAutoCompleteAdapter.clear();
            englishAutoCompleteAdapter.addAll(autoCompleteList);
            englishAutoCompleteAdapter.notifyDataSetChanged();
        }
    }

    public class ShowMalayalamResult extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            loader.setVisibility(View.VISIBLE);
            autocompleteListView.setVisibility(View.GONE);
            resultList2.clear();
        }

        protected String doInBackground(String... params) {
            Iterator keyIterator = keys.iterator();
            String key ;
            List<String> item;
            while (keyIterator.hasNext() ) {
                key = (String) keyIterator.next();
                item = multi_map.get( key );
                if(item.toString().contains(str)) {
                    for (String word : item) {
                        if (word.split(" - ")[1].equals(str))
                            resultList2.add(" •  "+key+"  -  "+wordTypes.get(word.split(" - ")[0]));
                    }
                }
            }
            mydb.insertHistory(str,resultList2.toString());
            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            englishResultAdapter.clear();
            englishResultAdapter.addAll(resultList2);
            englishResultAdapter.notifyDataSetChanged();
            searchResultListView.setVisibility(View.VISIBLE);
            linearResult.setVisibility(View.VISIBLE);
            tvResult.setText(resultList.size() + " Results");
            loader.setVisibility(View.GONE);
        }
    }

    public class ShowHistory extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            loader.setVisibility(View.VISIBLE);
            resultList2.clear();
        }

        protected String doInBackground(String... params) {
            resultList2 = mydb.getData();
            resultList3 = mydb.getMeaning();
            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            historyAdapter.clear();
            historyAdapter.addAll(resultList2);
            historyAdapter.notifyDataSetChanged();
            loader.setVisibility(View.GONE);
            layout_history.setVisibility(View.VISIBLE);
            historyListView.setVisibility(View.VISIBLE);
            if(resultList2.size() == 0)
                textHistorySummary.setText("No history found.");
            else
                textHistorySummary.setText(resultList2.size() + " Words");
            loader.setVisibility(View.GONE);
        }
    }

}

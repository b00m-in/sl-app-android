/*
* Copyright (C) 2019 Texas Instruments Incorporated - http://www.ti.com/
*
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions
*  are met:
*
*    Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
*    Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in the
*    documentation and/or other materials provided with the
*    distribution.
*
*    Neither the name of Texas Instruments Incorporated nor the names of
*    its contributors may be used to endorse or promote products derived
*    from this software without specific prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
*  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
*  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
*  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
*  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
*  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
*  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
*  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
*  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
*  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
*  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*
*/

package in.b00m.smartconfig;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import in.b00m.smartconfig.utils.NetworkUtil;
import in.b00m.smartconfig.utils.SharedPreferencesInterface_;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFragment(R.layout.login_fragment)
public class LoginPage  extends Fragment{

	private Logger mLogger;

	@Pref
	SharedPreferencesInterface_ prefs;

	@ViewById
	TextView resp;

	@ViewById
        EditText login_email_edittext;

	@ViewById
        EditText login_pswd_edittext;

	@AfterViews
	void afterViews() {
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int width = displaymetrics.widthPixels;

            //listen to editText focus and hiding keyboard when focus is out
            /*login_email_edittext.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                            if (!hasFocus) {
                                    hideKeyboard(v);
                            }
                    }
            });
            login_pswd_edittext.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                            if (!hasFocus) {
                                    hideKeyboard(v);
                            }
                    }
            });*/

            mLogger = LoggerFactory.getLogger(LoginPage.class);
	}

	@Click
	void buttonLogin() {
		//mPager.setAdapter(null);
		
		/*FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
		transaction.setCustomAnimations(R.anim.fragment_fade_out, R.anim.fragment_fade_out);
		transaction.remove(this);
		transaction.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		transaction.commit();*/
            //Boolean ok = PostLogin(login_email_edittext.getText().toString(), login_pswd_edittext.getText().toString());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    new GetLoginResult().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, login_email_edittext.getText().toString(), login_pswd_edittext.getText().toString());
            } else {
                    new GetLoginResult().execute(login_email_edittext.getText().toString(), login_pswd_edittext.getText().toString());
            }

	}

        Boolean PostLogin(String email, String pswd) {
            String baseUrl = "://pv.b00m.in";
            mLogger.info("*AP* Logging into cloud: " + email + pswd);
            Boolean result = NetworkUtil.loginToCloud(baseUrl, email, pswd);
            //mLogger.info("*AP* Logging into cloud: " + resultString);
            return result;

        }

	class GetLoginResult extends AsyncTask<String, Void, Boolean> {

		private Boolean mIsViaWifi;

		@Override
		protected void onPostExecute(Boolean result) {
                    mLogger.info("*AP* login result text: " + result);
                    super.onPostExecute(result);
                    //resp.setText(result.toString());
                    if (result) {
                        resp.setText("Logged in as " + prefs.sub().get());
                    } else {
                        resp.setText("There was a problem"); //result.toString());
                    }
                    
		}

		@Override
		protected Boolean doInBackground(String... params) {
			mLogger.info("GetLoginResult doInBackground called");
                        String baseUrl = "://pv.b00m.in";
                        mLogger.info("*AP* Logging into cloud: " + baseUrl + " " + params[0] + " " + params[1]);
                        Boolean result = NetworkUtil.loginToCloud(baseUrl, params[0], params[1]);
                        if (result) {
                            prefs.sub().put(params[0]);
                        }
			return result;
		}
	}
}

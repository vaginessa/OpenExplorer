package com.box.androidlib;

import com.box.androidlib.box2.AccessToken;

public interface GetAccessTokenListener extends ResponseListener {
	public void onComplete(AccessToken token);
}

package com.box.androidlib.box2;

import java.io.IOException;

public interface ResponseListener {
	void onComplete(JSONParent obj);
	void onIOException(IOException e);
}

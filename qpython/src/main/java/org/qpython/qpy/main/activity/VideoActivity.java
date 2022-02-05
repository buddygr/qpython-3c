package org.qpython.qpy.main.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.VideoView;

import org.qpython.qpy.R;

public class VideoActivity extends Activity
 {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
		Intent intent=getIntent();
		String path=intent.getStringExtra("path");
        VideoView videoView=findViewById(R.id.videoView);
		videoView.setVideoPath(path);
        MediaController mediaController=new MediaController(this);
		mediaController.setAnchorView(mediaController);
		videoView.start();
		videoView.setMediaController(mediaController);
    }
}

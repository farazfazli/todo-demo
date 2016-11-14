package com.farazfazli.todolist;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int AUTH_RESULT = 100;

    private ListView todoListView;
    private EditText todoInput;
    private TextView greetingTextView;

    private ValueEventListener listener;
    private DatabaseReference databaseReference;

    private FirebaseListAdapter<Todo> todoFirebaseListAdapter;
    private ArrayList<String> todoKeys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        todoKeys = new ArrayList<>();
        todoListView = (ListView) findViewById(R.id.todo_listview);
        todoInput = (EditText) findViewById(R.id.todo_input);
        greetingTextView = (TextView) findViewById(R.id.greeting_textview);

        if (FirebaseAuth.getInstance(FirebaseApp.getInstance()).getCurrentUser() == null) {
            // The current user doesn't exist, which means we are logged out
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), AUTH_RESULT);
        } else {
            initializeFirebase();
        }
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Tap a todo to toggle completion, long press to delete!")
                .setTitle("Instructions");
        builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void initializeFirebase() {
        initializeUI();
        addTodoListener();
        initializeListView();
    }

    private void initializeUI() {
        greetingTextView.setText(String.format("Welcome, %s", FirebaseAuth.getInstance().getCurrentUser().getEmail()));
        databaseReference = FirebaseDatabase.getInstance().getReference().child(FirebaseAuth.getInstance().getCurrentUser().getUid()).getRef();
        todoFirebaseListAdapter = new FirebaseListAdapter<Todo>(this, Todo.class, android.R.layout.simple_list_item_1, databaseReference) {
            @Override
            protected void populateView(View view, Todo model, int position) {
                ((TextView)view.findViewById(android.R.id.text1)).setText(model.getTodo());
                if (model.isCompleted()) {
                    ((TextView)view.findViewById(android.R.id.text1)).setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                }
            }
        };
    }

    private void initializeListView() {
        todoListView.setAdapter(todoFirebaseListAdapter);
        todoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i(TAG, todoKeys.get(i));
                TextView textview = (TextView) view.findViewById(android.R.id.text1);
                if (((textview).getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) <= 0) {
                    (textview).setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                    databaseReference.child(todoKeys.get(i)).child("completed").setValue(true);
                } else {
                    (textview).setPaintFlags(textview.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    databaseReference.child(todoKeys.get(i)).child("completed").setValue(false);
                }
            }
        });
        todoListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                databaseReference.child(todoKeys.get(i)).setValue(null);
                return false;
            }
        });
    }

    private void addTodoListener() {
        listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                todoKeys.clear(); // Clears existing keys
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    todoKeys.add(dataSnapshot1.getKey()); // Adds all of new keys to the ArrayList
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        databaseReference.addValueEventListener(listener);
    }

    // Called from Add button
    public void addTodoPressed(View view) {
        if (validTodo()) {
            addTodo();
        }
        clearTodoInput();
    }

    private boolean validTodo() {
        return todoInput.getText().toString().trim().length() > 0;
    }

    private void addTodo() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        databaseReference.push().setValue(new Todo(todoInput.getText().toString(), false));
    }

    private void clearTodoInput() {
        todoInput.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Always remember to clean up FirebaseUI List Adapter and event listeners in onDestroy
        todoFirebaseListAdapter.cleanup();
        databaseReference.removeEventListener(listener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Request code recognizes the result
        if (requestCode == AUTH_RESULT) {
            Log.i(TAG, "Received response");
            showDialog();
            initializeFirebase();
        }
    }
}

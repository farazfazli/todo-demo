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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

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

    // TAG to identify our Activity when we are looking at log messages
    private static final String TAG = MainActivity.class.getSimpleName();

    // Result code to identify our response from the Firebase sign-in/register
    // activity
    private static final int AUTH_RESULT = 100;

    // UI elements
    private ListView todoListView;
    private EditText todoInput;
    private TextView greetingTextView;

    // Firebase related
    private ValueEventListener listener;
    private DatabaseReference databaseReference;
    private FirebaseListAdapter<Todo> todoFirebaseListAdapter;

    // Firebase child keys
    private ArrayList<String> todoKeys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize our ArrayList to hold keys, and initialize UI components
        todoKeys = new ArrayList<>();
        todoListView = (ListView) findViewById(R.id.todo_listview);
        todoInput = (EditText) findViewById(R.id.todo_input);
        greetingTextView = (TextView) findViewById(R.id.greeting_textview);

        // Checks if the current user doesn't exist, which means they are logged out
        // if they are, start an Activity for sign-in provided by Firebase and get
        // back a result
        if (FirebaseAuth.getInstance(FirebaseApp.getInstance()).getCurrentUser() == null) {
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), AUTH_RESULT);
        } else {
            initializeFirebase();
        }
    }

    // Shows a dialog the very first time someone logs in, explaining to them
    // how to use the app - methods are refactored so that later down the line,
    // it's easier to change particular parts of the app
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
        initializeWelcomeMessage();
        initializeListView();
        addTodoKeysListener();
    }

    // Sets the welcome TextView
    private void initializeWelcomeMessage() {
        greetingTextView.setText(String.format("Welcome, %s", FirebaseAuth.getInstance().getCurrentUser().getEmail()));
        databaseReference = FirebaseDatabase.getInstance().getReference().child(FirebaseAuth.getInstance().getCurrentUser().getUid()).getRef();
    }

    // Initializes ListView, setting the adapter as well as
    // item click and item long click listeners
    private void initializeListView() {
        todoFirebaseListAdapter = new FirebaseListAdapter<Todo>(this, Todo.class, android.R.layout.simple_list_item_1, databaseReference) {
            @Override
            protected void populateView(View view, Todo model, int position) {
                ((TextView) view.findViewById(android.R.id.text1)).setText(model.getTodo());
                if (model.isCompleted()) {
                    ((TextView) view.findViewById(android.R.id.text1)).setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                }
            }
        };
        todoListView.setAdapter(todoFirebaseListAdapter);
        todoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i(TAG, todoKeys.get(i));
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (incomplete(textView)) {
                    markAsComplete(textView, i);
                } else {
                    markAsIncomplete(textView, i);
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

    // Listens for changes on todos and updates our todoKeys ArrayList accordingly
    private void addTodoKeysListener() {
        listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                todoKeys.clear(); // Clears existing keys
                for (DataSnapshot todo : dataSnapshot.getChildren()) {
                    todoKeys.add(todo.getKey()); // Adds all of new keys to the ArrayList
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        databaseReference.addValueEventListener(listener);
    }

    // Checks paint flags for if a particular item is incomplete
    private boolean incomplete(TextView textView) {
        return ((textView).getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) <= 0;
    }

    // Marks particular item as complete
    private void markAsComplete(TextView textView, int i) {
        (textView).setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
        databaseReference.child(todoKeys.get(i)).child("completed").setValue(true);
    }

    // Marks particular item as incomplete
    private void markAsIncomplete(TextView textView, int i) {
        (textView).setPaintFlags(textView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        databaseReference.child(todoKeys.get(i)).child("completed").setValue(false);
    }

    // Called from Add button
    public void addTodoPressed(View view) {
        if (validTodo()) {
            addTodo();
        }
        clearTodoInput();
    }

    // Checks if it's valid, right now just sees if it's not empty
    private boolean validTodo() {
        return todoInput.getText().toString().trim().length() > 0;
    }

    // After validation, it's added to Firebase
    private void addTodo() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        databaseReference.push().setValue(new Todo(todoInput.getText().toString(), false));
    }

    // Helper method to clear the input field
    private void clearTodoInput() {
        todoInput.setText("");
    }

    // Overriding lifecycle method, so that we can clean up Firebase UI List Adapter
    // and remove any Event Listeners
    @Override
    protected void onDestroy() {
        super.onDestroy();
        todoFirebaseListAdapter.cleanup();
        databaseReference.removeEventListener(listener);
    }

    // Called after logging in - we receive the result here
    // and initialize Firebase-related components, as we are logged in
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Response code tells us that it's successful, request code recognizes the result
        if (resultCode == RESULT_OK && requestCode == AUTH_RESULT) {
            Log.i(TAG, "Received successful response, showing dialog and initializing Firebase.");
            showDialog();
            initializeFirebase();
        }
    }
}

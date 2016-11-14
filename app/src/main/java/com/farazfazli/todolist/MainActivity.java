package com.farazfazli.todolist;

import android.app.Activity;
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
import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int AUTH_RESULT = 100;

    private ListView todoListView;
    private Button addButton;
    private EditText todoInput;
    private TextView greetingTextView;
    private String username;

    private ValueEventListener listener;
    private DatabaseReference databaseReference;

    private FirebaseListAdapter<Todo> todoFirebaseListAdapter;
    private ArrayList<String> keys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        keys = new ArrayList<>();
        todoListView = (ListView) findViewById(R.id.todo_listview);
        addButton = (Button) findViewById(R.id.add);
        todoInput = (EditText) findViewById(R.id.todo_input);
        greetingTextView = (TextView) findViewById(R.id.greeting_textview);


        if (FirebaseAuth.getInstance(FirebaseApp.getInstance()).getCurrentUser() == null) {
            // The current user doesn't exist, which means we are logged out
            // in this case, we need to go to the LoginActivity
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), AUTH_RESULT);
        } else {
            initializeFirebase();
        }
    }

    private void initializeFirebase() {
        username = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        greetingTextView.setText(String.format("Welcome, %s", username));
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

        listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    keys.add(dataSnapshot1.getKey());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        databaseReference.addValueEventListener(listener);

        todoListView.setAdapter(todoFirebaseListAdapter);
        todoListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i(TAG, keys.get(i));
                TextView textview = (TextView) view.findViewById(android.R.id.text1);
                if (((textview).getPaintFlags() & Paint.STRIKE_THRU_TEXT_FLAG) <= 0) {
                    (textview).setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
                    databaseReference.child(keys.get(i)).child("completed").setValue(true);
                } else {
                    (textview).setPaintFlags(textview.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    databaseReference.child(keys.get(i)).child("completed").setValue(false);
                }
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validTodo()) {
                    addTodo();
                }
                clearTodoInput();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Result code tells if result was okay, request code recognizes the result
        if (requestCode == AUTH_RESULT) {
            Log.i(TAG, "Received response");
            initializeFirebase();
        }
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
        // Always remember to clean up in onDestroy
        todoFirebaseListAdapter.cleanup();
        databaseReference.removeEventListener(listener);
    }
}

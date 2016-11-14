package com.farazfazli.todolist;

/**
 * Created by farazfazli on 11/13/16.
 */

public class Todo {
    private String todo;
    private boolean completed;

    public Todo() {

    }

    public Todo(String todo, boolean completed) {
        this.todo = todo;
        this.completed = completed;
    }

    public String getTodo() {
        return todo;
    }

    public void setTodo(String todo) {
        this.todo = todo;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}

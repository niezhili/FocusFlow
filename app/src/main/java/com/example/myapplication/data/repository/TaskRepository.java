package com.example.myapplication.data.repository;

import com.example.myapplication.data.dao.TaskDao;
import com.example.myapplication.data.entity.Task;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * TaskRepository - 任务仓库
 *
 * 封装 TaskDao，统一在 IO 线程执行数据库操作
 */
public class TaskRepository {

    private final TaskDao taskDao;

    public TaskRepository(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    public Flowable<List<Task>> getAllTasks() {
        return taskDao.getAllTasks().subscribeOn(Schedulers.io());
    }

    public Flowable<List<Task>> getActiveTasks() {
        return taskDao.getActiveTasks().subscribeOn(Schedulers.io());
    }

    public Flowable<List<Task>> getCompletedTasks() {
        return taskDao.getCompletedTasks().subscribeOn(Schedulers.io());
    }

    public Flowable<List<Task>> searchTasks(String keyword) {
        return taskDao.searchTasks(keyword).subscribeOn(Schedulers.io());
    }

    public Single<Task> getTaskById(long taskId) {
        return taskDao.getTaskById(taskId).subscribeOn(Schedulers.io());
    }

    public Completable insertTask(Task task) {
        task.setCreatedAt(System.currentTimeMillis());
        task.setUpdatedAt(System.currentTimeMillis());
        return taskDao.insertTask(task).subscribeOn(Schedulers.io());
    }

    public Completable updateTask(Task task) {
        task.setUpdatedAt(System.currentTimeMillis());
        return taskDao.updateTask(task).subscribeOn(Schedulers.io());
    }

    public Completable deleteTask(long taskId) {
        return taskDao.deleteTaskById(taskId).subscribeOn(Schedulers.io());
    }

    public Completable setTaskCompleted(long taskId, boolean completed) {
        return taskDao.setTaskCompleted(taskId, completed, System.currentTimeMillis())
                .subscribeOn(Schedulers.io());
    }

    public Completable addFocusSeconds(long taskId, long seconds) {
        return taskDao.addFocusSeconds(taskId, seconds, System.currentTimeMillis())
                .subscribeOn(Schedulers.io());
    }

    public Single<Integer> getTodayCompletedTaskCount(long dayStart, long dayEnd) {
        return taskDao.getTodayCompletedTaskCount(dayStart, dayEnd).subscribeOn(Schedulers.io());
    }
}
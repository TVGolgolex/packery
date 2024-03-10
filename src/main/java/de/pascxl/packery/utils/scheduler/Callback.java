package de.pascxl.packery.utils.scheduler;

/**
 * Created by Tareko on 24.05.2017.
 */
public interface Callback<C> {

    void call(C value);

}

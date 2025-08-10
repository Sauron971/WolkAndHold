package com.kyas.wolkandhold.routesfragment;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.kyas.wolkandhold.R;

import java.util.HashSet;
import java.util.Set;

public class RouteSwipeCallback extends ItemTouchHelper.SimpleCallback {
    private final RecyclerRoutesAdapter adapter;

    public RouteSwipeCallback(RecyclerRoutesAdapter adapter) {
        super(0, ItemTouchHelper.LEFT);
        this.adapter = adapter;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        adapter.showMenu(viewHolder.getAdapterPosition());
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {

        View content = ((RecyclerRoutesAdapter.ViewHolder) viewHolder).routeContent;
        float buttonWidth = dpToPx(content.getContext(), 82);

        float clampedDX = Math.max(-buttonWidth, Math.min(0f, dX));
        content.setTranslationX(clampedDX);
    }

    private int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

}

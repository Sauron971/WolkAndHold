package com.kyas.wolkandhold.routesfragment;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kyas.wolkandhold.R;
import com.kyas.wolkandhold.database.entities.Route;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecyclerRoutesAdapter extends RecyclerView.Adapter<RecyclerRoutesAdapter.ViewHolder> {

    private final LayoutInflater inflater;
    private List<Route> routes = new ArrayList<>();
    private final onRouteClickListener onClickListener;

    public RecyclerRoutesAdapter(Context context, onRouteClickListener onClickListener) {
        this.inflater = LayoutInflater.from(context);
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_route, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Route route = routes.get(position);
        holder.routeTitle.setText(route.name);
        DateFormat format = SimpleDateFormat.getDateInstance();
        holder.routeDate.setText(format.format(new Date(route.createdAt)));
        holder.routeDistance.setText(String.format(Locale.getDefault(), "%.4f м", route.distance));

        if (route.showMenu) {
            holder.routeContent.animate().translationX(-dpToPx(holder.routeContent.getContext(), 82)).start();
        } else {
            holder.routeContent.animate().translationX(0).start();
        }

        holder.deleteButton.setOnClickListener(v -> {
            holder.routeContent.animate()
                    .translationX(-holder.routeContent.getWidth())
                    .setDuration(300)
                    .withEndAction(() -> {
                        onClickListener.onDeleteButtonClick(route, position);
                    })
                    .start();
        });

        holder.itemView.setOnClickListener(v -> {
            onClickListener.onRouteClick(route, position);
        });
    }

    public void showMenu(int position) {
        for (int i = 0; i < routes.size(); i++) {
            routes.get(i).showMenu = (i == position);
        }
        notifyDataSetChanged();
    }

    public void closeMenu(int position) {
        routes.get(position).showMenu = false;
        notifyItemChanged(position);
    }

    public void addRoute(Route route) {
        routes.add(route);
        notifyItemInserted(routes.size() - 1);
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return routes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView routeTitle;
        final TextView routeDate;
        final TextView routeDistance;
        final ImageButton deleteButton;
        final LinearLayout routeContent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            routeTitle = itemView.findViewById(R.id.route_title);
            routeDate = itemView.findViewById(R.id.route_date);
            routeDistance = itemView.findViewById(R.id.route_distance);
            deleteButton = itemView.findViewById(R.id.delete_button);
            routeContent = itemView.findViewById(R.id.route_content);
        }
    }

    public interface onRouteClickListener {
        void onRouteClick(Route route, int position);
        void onDeleteButtonClick(Route route, int position);
    }

    private int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}

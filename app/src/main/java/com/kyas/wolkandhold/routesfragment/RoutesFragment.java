package com.kyas.wolkandhold.routesfragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kyas.wolkandhold.MainActivity;
import com.kyas.wolkandhold.R;
import com.kyas.wolkandhold.RouteViewModel;
import com.kyas.wolkandhold.database.AppDatabase;
import com.kyas.wolkandhold.database.dao.RouteDao;
import com.kyas.wolkandhold.database.dao.RoutePointDao;
import com.kyas.wolkandhold.database.entities.Route;
import com.kyas.wolkandhold.mapfragment.BufferedRoute;
import com.kyas.wolkandhold.mapfragment.MapFragment;
import com.yandex.mapkit.geometry.Point;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RoutesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RoutesFragment extends Fragment {

    private RecyclerView recyclerViewRoutes;
    private Activity activity;
    private Executor executor;

    public RoutesFragment() {
        // Required empty public constructor
    }

    public static RoutesFragment newInstance() {
        return new RoutesFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = requireActivity();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerViewRoutes = view.findViewById(R.id.routes_recycler);
        executor = Executors.newSingleThreadExecutor();
        RecyclerRoutesAdapter adapterRecycler = getRecyclerRoutesAdapter();
        recyclerViewRoutes.setAdapter(adapterRecycler);
        ItemTouchHelper helper = new ItemTouchHelper(new RouteSwipeCallback(adapterRecycler));
        helper.attachToRecyclerView(recyclerViewRoutes);

        RouteViewModel routeViewModel = new ViewModelProvider(this).get(RouteViewModel.class);
        routeViewModel.getRoutes().observe(getViewLifecycleOwner(), adapterRecycler::setRoutes);

    }

    @NonNull
    private RecyclerRoutesAdapter getRecyclerRoutesAdapter() {
        RecyclerRoutesAdapter.onRouteClickListener onRouteClickListener = new RecyclerRoutesAdapter.onRouteClickListener() {
            @Override
            public void onRouteClick(Route route, int position) {
                executor.execute(() -> {
                    Log.d("RouteClicked", "Clicked route on recycler " + route.toString());
                    RoutePointDao dao = AppDatabase.getInstance(activity.getApplication()).getRoutePointDao();
                    dao.getPointsForRoute(route.id).forEach((p) -> {
                        BufferedRoute.add(new Point(p.latitude, p.longitude));
                    });
                });

            }

            @Override
            public void onDeleteButtonClick(Route route, int position) {
                executor.execute(() -> {
                    RouteDao dao = AppDatabase.getInstance(activity.getApplication()).getRouteDao();
                    dao.delete(route);
                    Log.d("TAG", "onDeleteButtonClick: deleting");
                });
                Log.d("TAG", "onDeleteButtonClick: deleting2");
            }
        };
        return new RecyclerRoutesAdapter(activity, onRouteClickListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_routes, container, false);
    }


}
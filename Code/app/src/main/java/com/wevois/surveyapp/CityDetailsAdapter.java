package com.wevois.surveyapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wevois.surveyapp.databinding.DisplayCityListBinding;

import java.util.ArrayList;

public class CityDetailsAdapter extends RecyclerView.Adapter<CityDetailsAdapter.ParentViewHolder> {

    ArrayList<CityDetails> models;
    Context context;
    OnClickInterface onClickInterface;

    public CityDetailsAdapter(ArrayList<CityDetails> models, Context context,OnClickInterface onClickInterface){
        this.models = models;
        this.context = context;
        this.onClickInterface = onClickInterface;
    }

    @NonNull
    @Override
    public ParentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        DisplayCityListBinding binding = DisplayCityListBinding.inflate(layoutInflater,parent,false);

        return new ParentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ParentViewHolder holder, int position) {
        CityDetails model = models.get(position);
        holder.binding.setCity(model);
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    class ParentViewHolder extends RecyclerView.ViewHolder{
        DisplayCityListBinding binding;

        public ParentViewHolder(DisplayCityListBinding itemVeiw){
            super(itemVeiw.getRoot());
            this.binding = itemVeiw;
            itemVeiw.linearLay.setOnClickListener(view -> onClickInterface.onItemClick(getAdapterPosition()));
        }
    }
}
package com.example.pricecart.main

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pricecart.R
import com.example.pricecart.data.model.FavouriteItem
import com.example.pricecart.databinding.ItemFavouriteBinding

class FavouritesAdapter(
    private val onItemClicked: (FavouriteItem) -> Unit,
    private val onRemoveClicked: (FavouriteItem) -> Unit,
) : RecyclerView.Adapter<FavouritesAdapter.FavouriteViewHolder>() {
    private val items = mutableListOf<FavouriteItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteViewHolder {
        val binding = ItemFavouriteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return FavouriteViewHolder(binding, onItemClicked, onRemoveClicked)
    }

    override fun onBindViewHolder(holder: FavouriteViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<FavouriteItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class FavouriteViewHolder(
        private val binding: ItemFavouriteBinding,
        private val onItemClicked: (FavouriteItem) -> Unit,
        private val onRemoveClicked: (FavouriteItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(favouriteItem: FavouriteItem) {
            val context = binding.root.context
            binding.favouriteNameTextView.text = favouriteItem.productName
            binding.favouriteMetaTextView.text = favouriteItem.createdAt?.let { createdAt ->
                DateUtils.getRelativeTimeSpanString(
                    createdAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                )
            } ?: context.getString(R.string.saved_item_action_hint)
            binding.root.setOnClickListener {
                onItemClicked(favouriteItem)
            }
            binding.favouriteRemoveButton.setOnClickListener {
                onRemoveClicked(favouriteItem)
            }
        }
    }
}

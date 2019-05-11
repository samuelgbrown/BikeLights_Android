package to.us.suncloud.bikelights.common.BWA_Manager;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import to.us.suncloud.bikelights.common.BWA_Manager.SavedBWAFragment.savedBWAListener;
import to.us.suncloud.bikelights.R;
import to.us.suncloud.bikelights.common.Color.Bike_Wheel_Animation;
import to.us.suncloud.bikelights.common.Color.SavedBWA;
import to.us.suncloud.bikelights.common.ObservedRecyclerView;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link SavedBWA} and makes a call to the
 * specified {@link savedBWAListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class SavedBWARecyclerViewAdapter extends ObservedRecyclerView.Adapter<SavedBWARecyclerViewAdapter.bindViewHolder> {
    private static final String SEL = "SEL";

    private static final int BWA_VIEW_TYPE = 0;
    private static final int EMPTY_VIEW_TYPE = 0;

    private final List<SavedBWA> savedBWA_List;
    private boolean selectable; // Are the items selectable? (Is the user choosing a BWA to apply to a wheel?)
    private int selectedItem = -1;
//    private final savedBWAListener mListener;

    public SavedBWARecyclerViewAdapter(List<SavedBWA> savedBWA_List, boolean selectable) {
        this.savedBWA_List = savedBWA_List;
        this.selectable = selectable;
//        mListener = listener;
    }

    abstract class bindViewHolder extends RecyclerView.ViewHolder {
        public bindViewHolder(View itemView) {
            super(itemView);
        }
    }

    @Override
    public bindViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == BWA_VIEW_TYPE) {
            View view = inflater.inflate(R.layout.saved_bwa_item_view, parent, false);

            if (selectable) {
                // If the user is allowed to select items, make sure there is a highlighting action
                view.setBackground(parent.getContext().getDrawable(R.drawable.highlight_drawable));
            }

            return new BWA_ViewHolder(view);
        } else {
            // If there are no BWA's to display...
            return new emptyViewHolder(inflater.inflate(R.layout.simple_text_view, parent, false));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (savedBWA_List.size() == 0) {
            return EMPTY_VIEW_TYPE;
        } else {
            return BWA_VIEW_TYPE;
        }
    }

    @Override
    public void onBindViewHolder(final bindViewHolder holder, int position) {
        if (holder instanceof BWA_ViewHolder) {
            ((BWA_ViewHolder) holder).bind(position);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull bindViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);

        try {
            if (!payloads.isEmpty()) {
                // Go though the entire payload
                for (int i = 0; i < payloads.size(); i++) {
                    if (payloads.get(i).equals(SEL)) {
                        // If any of the payloads is the string SEL, then it indicates that this holder's selection status has changed
                        if (holder instanceof BWA_ViewHolder) {
                            ((BWA_ViewHolder) holder).view.setSelected(position == selectedItem); // Set whether or not this is selected
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        // There is always at least one item view (if savedBWA_List is empty, then the empty viewholder should be displayed)
//        int count = Math.max(1, savedBWA_List.size());
        int count = savedBWA_List.size();
        return count;
    }

    public class BWA_ViewHolder extends bindViewHolder {
        public final View view;
        public final TextView nameView;
        public final ImageView remove;
        public final ImageView modify;

        public BWA_ViewHolder(View view) {
            super(view);
            this.view = view;
            nameView = view.findViewById(R.id.name_bwa);
            remove = view.findViewById(R.id.remove_bwa);
            modify = view.findViewById(R.id.modify_bwa);

            // TODO: Add a modification button (gear) to change the name of the BWA; use the bwa_bookmark AlertDialog that was used in WheelViewActivity
        }

        public void bind(final int position) {
            if (selectable) {
                // If the items in this list are selectable, set a click listener for selection
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int oldSelectedItem = selectedItem;

                        if (selectedItem != getLayoutPosition()) {
                            // If the current item is NOT selected, then select it!
                            selectedItem = getLayoutPosition();

                            // If there was a previously selected item, then deselect it
                            if (selectedItem != -1) {
                                notifyItemChanged(oldSelectedItem, SEL);
                            }
                        } else {
                            // If the current item IS selected, then deselect it
                            selectedItem = -1;
                        }

                        // Notify this view that there has been a selection change
                        notifyItemChanged(getLayoutPosition(), SEL);

                    }
                });
            }

            String name = savedBWA_List.get(getLayoutPosition()).getSaveName();
            if (!name.isEmpty()) {
                nameView.setText(name);
                nameView.setTypeface(null, Typeface.NORMAL);
            } else {
                nameView.setText(view.getResources().getString(R.string.bwa_empty_name));
                nameView.setTypeface(null, Typeface.ITALIC);
            }
            // TODO: Eventually, a list of "pre-bookmarked" animations may be put in, in which case they cannot be deleted
            remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(remove.getContext())
                            .setMessage(remove.getContext().getResources().getString(R.string.dialog_deleted_BWA))
                            .setPositiveButton(remove.getContext().getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    savedBWA_List.remove(getLayoutPosition());

                                    if (savedBWA_List.size() > 0) {
                                        notifyItemRemoved(getLayoutPosition());
                                    } else {
                                        // If there are no more saveBWA's, then let the RecyclerView reevaluate its entire life
//                                        recyclerView.setAdapter(SavedBWARecyclerViewAdapter.this); // Force a redraw of all of the recyclerViews, because Android is bullshit and there seems to be no other goddamn way to show my goddamn empty View
                                        notifyDataSetChanged();
                                        // TODO: Fuck all this motherfucking bullshit and just implement the stupid motherfucking ObservableAdapter, motherfucking shit fuck damn
                                    }
                                }
                            })
                            .setNegativeButton(remove.getContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Do nothing
                                }
                            })
                            .show();
                }
            });

            modify.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // If this BWA does NOT exist in the bookmarks, allow the user to same the animation and save it
                    LayoutInflater inflater = (LayoutInflater) v.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View layoutView = inflater.inflate(R.layout.layout_bwa_bookmark, null); // First, inflate the view, so that the editText can be extracted
                    final EditText bookmarkText = layoutView.findViewById(R.id.bookmark_name);
                    bookmarkText.setText(savedBWA_List.get(getLayoutPosition()).getSaveName()); // Populate the EditText with the current name

                    final InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                    new AlertDialog.Builder(v.getContext())
                            .setView(layoutView)
                            .setPositiveButton(v.getResources().getString(R.string.save), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Hide the keyboard (god, it should not be this damn complicated...)
                                    imm.hideSoftInputFromWindow(bookmarkText.getWindowToken(), 0);

                                    // Save this BWA with the String from the dialog
                                    savedBWA_List.get(getLayoutPosition()).setSaveName(bookmarkText.getText().toString());

                                    // Let the RecyclerView know that something has changed
                                    notifyItemChanged(getLayoutPosition());
                                }
                            })
                            .setNegativeButton(v.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Hide the keyboard (ugh...)
                                    imm.hideSoftInputFromWindow(bookmarkText.getWindowToken(), 0);
                                }
                            })
                            .show();

                    // Make the EditText show the keyboard
                    bookmarkText.requestFocus();
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
            });
        }

//        @Override
//        public String toString() {
//            return super.toString() + " '" + remove.getText() + "'";
//        }
    }

    class emptyViewHolder extends bindViewHolder {
        TextView view;

        public emptyViewHolder(View itemView) {
            super(itemView);

            view = itemView.findViewById(R.id.simpleTextView);
            view.setText(itemView.getResources().getText(R.string.bwa_empty));
        }
    }

    public boolean hasSelection() {
        return selectedItem != -1;
    }

    public SavedBWA getSelectedBWA() {
        if (hasSelection()) {
            return savedBWA_List.get(selectedItem);
        } else {
            if (savedBWA_List.size() > 0) {
                return savedBWA_List.get(0);
            } else {
                return new SavedBWA("", new Bike_Wheel_Animation(120));
            }
        }
    }

    public List<SavedBWA> getSavedBWA_List() {
        return savedBWA_List;
    }
}

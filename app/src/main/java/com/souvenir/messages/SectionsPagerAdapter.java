package com.souvenir.messages;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class SectionsPagerAdapter extends FragmentStateAdapter {
    public SectionsPagerAdapter(@NonNull MainActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return position == 0 ? new ContactsFragment() : new ConversationsFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}

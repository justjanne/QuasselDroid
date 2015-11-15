/*
    QuasselDroid - Quassel client for Android
    Copyright (C) 2015 Ken Børge Viktil
    Copyright (C) 2015 Magnus Fjell
    Copyright (C) 2015 Martin Sandsmark <martin.sandsmark@kde.org>

    This program is free software: you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 3 of the License, or (at your option)
    any later version, or under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either version 2.1 of
    the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License and the
    GNU Lesser General Public License along with this program.  If not, see
    <http://www.gnu.org/licenses/>.
 */

package com.iskrembilen.quasseldroid.gui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.idunnololz.widgets.AnimatedExpandableListView;
import com.iskrembilen.quasseldroid.protocol.state.Buffer;
import com.iskrembilen.quasseldroid.protocol.state.IrcMode;
import com.iskrembilen.quasseldroid.protocol.state.IrcUser;
import com.iskrembilen.quasseldroid.protocol.state.NetworkCollection;
import com.iskrembilen.quasseldroid.R;
import com.iskrembilen.quasseldroid.protocol.state.UserCollection;
import com.iskrembilen.quasseldroid.events.BufferDetailsChangedEvent;
import com.iskrembilen.quasseldroid.events.BufferOpenedEvent;
import com.iskrembilen.quasseldroid.events.NetworksAvailableEvent;
import com.iskrembilen.quasseldroid.events.UserClickedEvent;
import com.iskrembilen.quasseldroid.util.BusProvider;
import com.iskrembilen.quasseldroid.util.ThemeUtil;
import com.squareup.otto.Subscribe;

import java.io.Serializable;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class NickListFragment extends Fragment implements Serializable {
    private NicksAdapter adapter;
    private AnimatedExpandableListView list;
    private int bufferId = -1;
    private NetworkCollection networks;
    private final String TAG = NickListFragment.class.getSimpleName();
    public String topic;

    private BacklogObserver observer = new BacklogObserver();

    public static NickListFragment newInstance() {
        return new NickListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new NicksAdapter();

        if (savedInstanceState != null) {
            bufferId = savedInstanceState.getInt("bufferid");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_nicks, container, false);
        list = (AnimatedExpandableListView) root.findViewById(R.id.userList);
        list.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                if (list.isGroupExpanded(groupPosition)) {
                    list.collapseGroupWithAnimation(groupPosition);
                } else {
                    list.expandGroupWithAnimation(groupPosition);
                }
                return true;
            }
        });
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        list.setAdapter(adapter);

    }

    @Override
    public void onStart() {
        super.onStart();
        BusProvider.getInstance().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        adapter.stopObserving();
        BusProvider.getInstance().unregister(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("bufferid", bufferId);
        super.onSaveInstanceState(outState);
    }

    private void queryUser(String nick) {
        BusProvider.getInstance().post(new UserClickedEvent(bufferId, nick));
    }

    public void setNetworks(NetworkCollection networks) {
        this.networks = networks;
    }

    public class NicksAdapter extends AnimatedExpandableListView.AnimatedExpandableListAdapter implements Observer {

        private LayoutInflater inflater;
        private UserCollection users;

        public NicksAdapter() {
            inflater = getActivity().getLayoutInflater();
            this.users = null;
        }

        public void setUsers(UserCollection users) {
            users.addObserver(this);
            this.users = users;
            notifyDataSetChanged();
            for (int i = 0; i < getGroupCount(); i++) {
                list.expandGroup(i);
            }
        }

        @Override
        public void update(Observable observable, Object data) {
            if (data == null) {
                return;
            }
            switch ((Integer) data) {
                case R.id.BUFFERUPDATE_USERSCHANGED:
                    notifyDataSetChanged();
                    break;
            }
        }

        public void stopObserving() {
            if (users != null)
                users.deleteObserver(this);

        }

        @Override
        public IrcUser getChild(int groupPosition, int childPosition) {
            return getGroup(groupPosition).second.get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return groupPosition * Integer.MAX_VALUE + childPosition;
        }

        @Override
        public View getRealChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            ViewHolderChild holder = null;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.widget_nick_single, null);
                holder = new ViewHolderChild();
                holder.nickView = (TextView) convertView.findViewById(R.id.nicklist_nick_view);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolderChild) convertView.getTag();
            }
            final IrcUser entry = getChild(groupPosition, childPosition);
            final IrcMode mode = getGroup(groupPosition).first;
            convertView.setBackgroundColor(ThemeUtil.getNickBg(mode));

            holder.nickView.setText(entry.nick);
            if (entry.away) {
                holder.nickView.setTextColor(ThemeUtil.Color.bufferParted);
            } else {
                holder.nickView.setTextColor(ThemeUtil.Color.bufferRead);
            }

            holder.nickView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    queryUser(entry.nick);
                }
            });

            return convertView;
        }

        @Override
        public int getRealChildrenCount(int groupPosition) {
            if (this.users == null) return 0;
            return getGroup(groupPosition).second.size();
        }

        @Override
        public Pair<IrcMode, List<IrcUser>> getGroup(int groupPosition) {
            int counter = 0;
            for (IrcMode mode : IrcMode.values()) {
                if (counter == groupPosition) {
                    return new Pair<IrcMode, List<IrcUser>>(mode, users.getUniqueUsersWithMode(mode));
                } else {
                    counter++;
                }
            }
            return null;
        }

        @Override
        public int getGroupCount() {
            if (users == null) return 0;
            return IrcMode.values().length;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            ViewHolderGroup holder = null;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.widget_nick_group, null);
                holder = new ViewHolderGroup();
                holder.nameView = (TextView) convertView.findViewById(R.id.nicklist_group_name_view);
                holder.countView = (TextView) convertView.findViewById(R.id.nicklist_group_count_view);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolderGroup) convertView.getTag();
            }
            Pair<IrcMode, List<IrcUser>> group = getGroup(groupPosition);
            convertView.setBackgroundColor(ThemeUtil.getNickBg(group.first));
            holder.nameView.setTextColor(ThemeUtil.getModeColor(group.first));
            holder.countView.setTextColor(ThemeUtil.getModeColor(group.first));

            if (group.second.size() < 1) {
                convertView.setVisibility(View.GONE);
                holder.nameView.setVisibility(View.GONE);
                holder.countView.setVisibility(View.GONE);
            } else {
                convertView.setVisibility(View.VISIBLE);
                holder.nameView.setVisibility(View.VISIBLE);
                holder.countView.setVisibility(View.VISIBLE);
                holder.nameView.setText(getResources().getQuantityString(group.first.modeName, group.second.size()));
                holder.countView.setText(group.first.icon + " " + group.second.size());
            }
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }


    public static class ViewHolderChild {
        public TextView nickView;
    }

    public static class ViewHolderGroup {
        public TextView nameView;
        public TextView countView;
    }

    @Subscribe
    public void onNetworksAvailable(NetworksAvailableEvent event) {
        if (event.networks != null) {
            this.networks = event.networks;
            if (bufferId != -1) {
                observer.setBuffer(networks.getBufferById(bufferId));
                updateUsers();
                BusProvider.getInstance().post(new BufferDetailsChangedEvent(bufferId));
            }
        }
    }

    @Subscribe
    public void onBufferOpened(BufferOpenedEvent event) {
        if (event.bufferId != -1) {
            this.bufferId = event.bufferId;
            if (networks != null) {
                observer.setBuffer(networks.getBufferById(bufferId));
                updateUsers();
                BusProvider.getInstance().post(new BufferDetailsChangedEvent(bufferId));
            }
        }
    }

    private void updateUsers() {
        Buffer buffer = networks.getBufferById(bufferId);
        if (buffer != null) {
            adapter.setUsers(buffer.getUsers());
        }
    }

    @Subscribe
    public void onBufferDetailsChanged(BufferDetailsChangedEvent event) {
        if (event.bufferId==bufferId) {
            Buffer buffer = networks.getBufferById(bufferId);
            if (buffer != null) {
                observer.setBuffer(buffer);
            }
        }
    }

    public class BacklogObserver implements Observer {
        private Buffer buffer;
        public void setBuffer(Buffer buffer) {
            if (this.buffer!=null) this.buffer.deleteObserver(this);
            this.buffer = buffer;
            if (this.buffer!=null) this.buffer.addObserver(this);
        }
        @Override
        public void update(Observable observable, Object data) {
            if (data == null) {
                return;
            }
            switch ((Integer) data) {
                case R.id.BUFFERUPDATE_TOPICCHANGED:
                    BusProvider.getInstance().post(new BufferDetailsChangedEvent(bufferId));
                    break;
                default:
            }
        }
    }
}

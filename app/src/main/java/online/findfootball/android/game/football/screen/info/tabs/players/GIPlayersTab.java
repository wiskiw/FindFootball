package online.findfootball.android.game.football.screen.info.tabs.players;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;

import online.findfootball.android.R;
import online.findfootball.android.app.App;
import online.findfootball.android.firebase.database.DataInstanceResult;
import online.findfootball.android.firebase.database.DatabaseLoader;
import online.findfootball.android.firebase.database.DatabasePackableInterface;
import online.findfootball.android.game.GameObj;
import online.findfootball.android.game.GameTeam;
import online.findfootball.android.game.football.object.FootballPlayer;
import online.findfootball.android.game.football.object.FootballTeams;
import online.findfootball.android.game.football.screen.info.tabs.players.recyclerview.PlayerListAdapter;
import online.findfootball.android.user.AppUser;
import online.findfootball.android.user.UserObj;

/**
 * A simple {@link Fragment} subclass.
 */
public class GIPlayersTab extends Fragment {

    private static final String TAG = App.G_TAG + ":GIPlayersTab";


    private FootballTeams teams;
    private GameObj thisGameObj;
    private AppUser thisAppUser;

    private DatabaseLoader.OnListenListener aListener;
    private DatabaseLoader aLoader;

    private Button joinLeaveBtn;
    private TextView playersCountTextView;


    private PlayerListAdapter mAdapter;
    private RecyclerView recyclerView;

    private int loadedCount;

    public GIPlayersTab() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        startListeningTeams();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopListeningTeams();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.gi_tab_players, container, false);
        playersCountTextView = (TextView) rootView.findViewById(R.id.count_text);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.player_list);
        joinLeaveBtn = (Button) rootView.findViewById(R.id.join_leave_btn);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new PlayerListAdapter();
        recyclerView.setAdapter(mAdapter);

        joinLeaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButtonEnable(false);
                thisAppUser = AppUser.getInstance(getContext());
                if (thisAppUser != null) {
                    afterUserAuth(thisAppUser);
                }
            }
        });
        return rootView;
    }

    private void updatePlayerNumb() {
        if (teams != null) {
            String str = teams.getTeamsOccupancy() + "/" + teams.getTeamsCapacity();
            playersCountTextView.setText(str);
        }
    }

    public void setData(GameObj game) {
        thisGameObj = game;
        teams = game.getTeams();
        startListeningTeams();
    }

    private DatabaseLoader.OnListenListener getListener(final GameTeam<FootballPlayer> team) {
        return new DatabaseLoader.OnListenListener() {
            @Override
            public void onChildAdded(final DataSnapshot dataSnapshot) {
                final String uid = dataSnapshot.getKey();
                UserObj u = new UserObj(uid);
                u.load(new DatabaseLoader.OnLoadListener() {
                    @Override
                    public void onComplete(DataInstanceResult result, DatabasePackableInterface packable) {
                        if (result.getCode() == DataInstanceResult.CODE_SUCCESS) {
                            FootballPlayer player = new FootballPlayer((UserObj) packable);
                            player.unpack(dataSnapshot);
                            team.addPlayer(player);
                            if (mAdapter != null) {
                                if (thisAppUser == null) {
                                    thisAppUser = AppUser.getInstance(getContext());
                                }
                                if (thisAppUser != null && thisAppUser.getUid().equals(player.getUid())) {
                                    mAdapter.addAppUserPlayer(player);
                                } else {
                                    mAdapter.addPlayer(player);
                                }
                            }
                            loadedCount++;
                            updatePlayerNumb();
                            tryEnableButton(player);
                        }
                    }
                });
            }

            @Override
            public void onChildChanged(final DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                FootballPlayer player = new FootballPlayer(dataSnapshot.getKey());
                team.removePlayer(player);
                if (mAdapter != null) {
                    if (thisAppUser == null) {
                        thisAppUser = AppUser.getInstance(getContext());
                    }
                    if (thisAppUser != null && thisAppUser.getUid().equals(player.getUid())) {
                        mAdapter.removeAppUserPayer(player);
                    } else {
                        mAdapter.removePlayer(player);
                    }
                }
                loadedCount--;
                updatePlayerNumb();
                tryEnableButton(player);
            }
        };
    }

    private void startListeningTeams() {
        if (teams == null) {
            return;
        }

        updatePlayerNumb();
        loadedCount = 0;

        if (thisGameObj.getTeams().getTeamsOccupancy() > 0) {
            // выключаем кнопку Join/Leave и ждем загрузки пользователя
            setButtonEnable(false);
        }
        if (aListener == null) {
            aListener = getListener(teams.getTeamA());
        }

        if (aLoader == null) {
            aLoader = new DatabaseLoader();
        }
        aLoader.listen(teams.getTeamA().getPlayerList(), aListener);
    }

    private void stopListeningTeams() {
        if (aLoader == null) {
            return;
        }
        aLoader.abortAllLoadings();
    }

    private void afterUserAuth(AppUser appUser) {
        FootballPlayer player = new FootballPlayer(appUser.getUid());
        if (teams.hasPlayer(player)) {
            teams.getTeamA().unrollPlayer(player); // убираем игрока из команды
            appUser.leaveGame(thisGameObj); // убираем игру у пользователя
        } else {
            // TODO : делить на команды тут
            teams.getTeamA().enrollPlayer(player); // добавляем игрока в команду
            appUser.joinGame(thisGameObj); // добавляем игру к польщователю
        }
    }

    private void tryEnableButton(FootballPlayer player) {
        if (thisAppUser == null) {
            thisAppUser = AppUser.getInstance(getContext());
        }
        if ((thisAppUser != null && thisAppUser.getUid().equals(player.getUid()))
                || loadedCount >= thisGameObj.getTeams().getTeamsCapacity()) {
            // Если загрузили AppUser'a или все игроки были загружены
            setButtonEnable(true);
        }
    }

    private void setButtonEnable(boolean b) {
        if (joinLeaveBtn != null) {
            joinLeaveBtn.setEnabled(b);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppUser.AUTH_REQUEST_CODE) {
            switch (resultCode) {
                case AppUser.RESULT_SUCCESS:
                    AppUser appUser = AppUser.getInstance(getContext(), false);
                    if (appUser != null) {
                        afterUserAuth(appUser);
                    }
                    break;
            }
        }

    }
}

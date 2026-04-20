package vnuhcm.hcmus.movify.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import vnuhcm.hcmus.movify.R;
import vnuhcm.hcmus.movify.fragments.BookingFragment;
import vnuhcm.hcmus.movify.fragments.BottomNavFragment;
import vnuhcm.hcmus.movify.fragments.HistoryFragment;
import vnuhcm.hcmus.movify.fragments.MapBookingFragment;
import vnuhcm.hcmus.movify.fragments.MessageFragment;
import vnuhcm.hcmus.movify.fragments.NotificationFragment;
import vnuhcm.hcmus.movify.fragments.ProfileFragment;

public class HomeActivity extends AppCompatActivity implements BottomNavFragment.MainNavigator {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        if (savedInstanceState == null) {
            openBooking();
        }
    }

    private void loadScreen(Fragment fragment, String tab) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentContainer, fragment)
                .commit();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.bottomNavContainer, BottomNavFragment.newInstance(tab))
                .commit();
    }

    @Override
    public void openBooking() {
        loadScreen(new BookingFragment(), BottomNavFragment.TAB_BOOKING);
    }

    @Override
    public void openProfile() {
        loadScreen(new ProfileFragment(), BottomNavFragment.TAB_PROFILE);
    }

    @Override
    public void openNotification() {
        loadScreen(new NotificationFragment(), BottomNavFragment.TAB_NOTIFICATION);
    }

    @Override
    public void openHistory() {
        loadScreen(new HistoryFragment(), BottomNavFragment.TAB_HISTORY);
    }

    @Override
    public void openMessage() {
        loadScreen(new MessageFragment(), BottomNavFragment.TAB_MESSAGE);
    }

    public void openMapBooking() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.contentContainer, new MapBookingFragment())
                .addToBackStack(null)
                .commit();

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.bottomNavContainer, BottomNavFragment.newInstance(BottomNavFragment.TAB_BOOKING))
                .commit();
    }
}

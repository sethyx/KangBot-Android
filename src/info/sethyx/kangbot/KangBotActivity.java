
package info.sethyx.kangbot;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ListActivity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;

public class KangBotActivity extends ListActivity implements LoaderCallbacks<Cursor> {

    private static final String TAG = "KangBotActivity";
    protected static PushLogAdapter mAdapter;
    protected Cursor mCursor;
    static Context mContext;
    Activity mActivity;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                FragmentManager fragmentManager = mActivity.getFragmentManager();
                SettingsDialogFragment newFragment = new SettingsDialogFragment();
                newFragment.show(fragmentManager, "settings");
                return true;
            case R.id.action_register:
                GCMHelper.register(mContext);
                return true;
            case R.id.action_unregister:
                GCMRegistrar.unregister(mContext);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();
        GCMHelper.checkVersionUpdateOnStart(mContext);
        mActivity = this;
        CursorLoader loader = new CursorLoader(this, BuildLogProvider.CONTENT_URI, new String[] {
                        BuildLogProvider._ID,
                        BuildLogProvider.C_TIME,
                        BuildLogProvider.C_DEVICE,
                        BuildLogProvider.C_SRC,
                        BuildLogProvider.C_RESULT,
                        BuildLogProvider.C_ERRLOG
                }, null, null, null);

        mCursor = loader.loadInBackground();
        mAdapter = new PushLogAdapter(mContext, mCursor,
                0);
        setListAdapter(mAdapter);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(this, BuildLogProvider.CONTENT_URI,
                null, null, null, null);
    }

    public class PushLogAdapter extends CursorAdapter {

        public PushLogAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);
        }

        class ViewHolder {
            public TextView device;
            public TextView time;
            public TextView result;
        }

        public int getId(int position) {
            Cursor tCursor = getCursor();
            tCursor.moveToPosition(position);
            return tCursor.getInt(0);
        }

        private Cursor getCursorAtPosition(int position) {
            Cursor tCursor = getCursor();
            tCursor.moveToPosition(position);
            return tCursor;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.push_log_row, null, true);

            ViewHolder holder = new ViewHolder();
            holder.device = (TextView) rowView.findViewById(R.id.device);
            holder.time = (TextView) rowView.findViewById(R.id.time);
            holder.result = (TextView) rowView.findViewById(R.id.result);
            rowView.setTag(holder);

            return rowView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();

            String device = cursor.getString(2);
            holder.device.setText(device);

            String time = (String) DateUtils.getRelativeDateTimeString(mContext,
                    cursor.getLong(1),
                    DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
            holder.time.setText(time);
            holder.result.setText(GCMHelper.getBuildStringWithColor(context,
                    cursor.getString(4), cursor.getString(3)), BufferType.SPANNABLE);
        }
    }

    @Override
    public void onResume() {
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
        if (PrefsHelper.getUser(mContext).isEmpty()) {
            FragmentManager fragmentManager = mActivity.getFragmentManager();
            SettingsDialogFragment newFragment = new SettingsDialogFragment();
            newFragment.show(fragmentManager, "settings");
        }
        super.onResume();
    }

    public static class BuildDetailDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.log_dialog, null);
            TextView error = (TextView) view.findViewById(R.id.log_text);
            error.setText(getArguments().getString("fullLog"));
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(view)
                    .setTitle(R.string.error_details)
                    .setPositiveButton("All to clipboard", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ClipboardManager clipboard = (ClipboardManager)
                                    mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                            clipboard.setPrimaryClip(ClipData.newPlainText("BuildLog",
                                    getArguments().getString("fullLog")));
                            Toast.makeText(mContext, "Copied!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("OK", null);
            return builder.create();
        }
    }

    public static class SettingsDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            final View view = inflater.inflate(R.layout.settings_dialog, null);
            final EditText user = (EditText) view.findViewById(R.id.user);
            final EditText pwd = (EditText) view.findViewById(R.id.pwd);
            user.setText(PrefsHelper.getUser(mContext));
            pwd.setText(PrefsHelper.getPwd(mContext));

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setView(view)
                    .setTitle(R.string.credentials)
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            PrefsHelper.putUser(mContext, user.getText().toString());
                            PrefsHelper.putPwd(mContext, pwd.getText().toString());
                            Toast.makeText(mContext, R.string.settings_saved, Toast.LENGTH_SHORT)
                                    .show();
                        }
                    })
                    .setNegativeButton("Cancel", null);
            return builder.create();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (GCMRegistrar.isRegistered(mContext)) {
            GCMRegistrar.onDestroy(mContext);
        }
    }

    @Override
    public void onLoadFinished(android.content.Loader<Cursor> arg0, Cursor arg1) {
        mAdapter.swapCursor(arg1);
        ListView mList = getListView();
        mList.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                /*
                 * getActivity().getContentResolver().delete(PushLogProvider.
                 * CONTENT_URI, PushLogProvider._ID + "=?", new String[] {
                 * String.valueOf(arg3) });
                 */
                return false;
            }
        });
        mList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                Cursor row = mAdapter.getCursorAtPosition(arg2);
                String fullLog = GCMHelper.buildErrorText(mContext, row.getString(5),
                        row.getLong(1), row.getString(4), row.getString(2), row.getString(3));
                FragmentManager fragmentManager = mActivity.getFragmentManager();
                BuildDetailDialogFragment newFragment = new BuildDetailDialogFragment();
                Bundle logBundle = new Bundle();
                logBundle.putString("fullLog", fullLog);
                newFragment.setArguments(logBundle);
                newFragment.show(fragmentManager, "details");
            }
        });
    }

    @Override
    public void onLoaderReset(android.content.Loader<Cursor> arg0) {
        Log.i(TAG, "onLoaderReset called");
        mAdapter.swapCursor(null);
    }
}

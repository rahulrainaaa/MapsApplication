package demo.maps.app.mapsapplication;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @class MapsActivity
 * @desc Handles the Google Map Demo UI completely.
 */

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback, LocationListener, GoogleMap.OnMarkerClickListener, RoutingListener {

    //Map and Location objects
    private GoogleMap mMap;
    LocationManager locationManager = null;
    Polyline line = null;

    //Map point markers
    Marker marker = null;
    Marker srchMarker = null;

    //Location points
    LatLng locStart = null;
    LatLng locEnd = null;

    //Boolean flags
    boolean firstSearch = true;
    boolean firstRefresh = true;

    /**
     * @desc FragmentActivity required methods override.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        //Load UI content.
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        //Check for permissions
        int check1 = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        int check2 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if((check1 != PackageManager.PERMISSION_GRANTED) || (check2 != PackageManager.PERMISSION_GRANTED))
        {
            Toast.makeText(getApplicationContext(), "Go to permissions and \nEnable all permission", Toast.LENGTH_LONG).show();
            startInstalledAppDetailsActivity();
            return;
        }

        //Reset all data and flags
        LatLng locStart = null;
        LatLng locEnd = null;
        boolean firstSearch = true;
        boolean firstRefresh = true;

        //Initialize location listener
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, this);
        Toast.makeText(getApplicationContext(), "Fetching Current Location", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause()
    {
        //Reset all data and flags
        LatLng locStart = null;
        LatLng locEnd = null;
        boolean firstSearch = true;
        boolean firstRefresh = true;

        //Remove the location listener callback
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(this);
        }
        locationManager = null;
        super.onPause();
    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        //Set GoogleMap attributes and listeners required
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.setOnMarkerClickListener(this);
    }

    /**
     * @desc LocationListener Interface Methods implemented.
     */

    @Override
    public void onLocationChanged(Location location) {

        //Get location and update map pin
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);

        //Check if refreshed first time.
        if(firstRefresh)
        {
            //Add Start Marker.
            marker = mMap.addMarker(new MarkerOptions().position(latLng).title("Current Position").icon(BitmapDescriptorFactory.fromResource(R.drawable.ba)));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            firstRefresh = false;
        }
        else
        {
            marker.setPosition(latLng);
        }
        locStart = new LatLng(latitude, longitude);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    /**
     * @desc MapMarker Interface Methods Implemented.
     */

    @Override
    public boolean onMarkerClick(Marker marker)
    {
        if(marker.getTitle().contains("Destination"))
        {
            //Do Routing
            Routing routing = new Routing.Builder()
                    .travelMode(Routing.TravelMode.DRIVING)
                    .withListener(MapsActivity.this)
                    .waypoints(locStart, locEnd)
                    .build();
            routing.execute();
        }
        else if(marker.getTitle().contains("Current"))
        {
            //Do find some place
            inputTextBox();
        }
        return false;
    }

    /**
     * @method getLocationFromAddress
     * @param strAddress Address/Location String
     * @desc Get searched location points from address and plot/update on map.
     */
    public void getLocationFromAddress(String strAddress)
    {
        //Location String to location points
        Geocoder coder = new Geocoder(this);
        List<Address> address;
        try {
            address = coder.getFromLocationName(strAddress,5);
            if (address==null) {
            }
            Address location=address.get(0);
            locEnd = new LatLng(location.getLatitude(), location.getLongitude());

            //If searched first time
            if(firstSearch)
            {
                firstSearch = false;
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                srchMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Destination").icon(BitmapDescriptorFactory.fromResource(R.drawable.bb)));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            }
            else
            {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                LatLng latLng = new LatLng(latitude, longitude);
                srchMarker.setTitle("Destination");
                srchMarker.setPosition(latLng);

                mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            }
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            //If line not null then remove old polyline routing.
            if(line != null)
            {
                line.remove();
            }

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * @method inputTextBox
     * @desc Prompts for Address to be searched on map.
     */
    public void inputTextBox()
    {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);

        alertDialogBuilder.setView(input);
        alertDialogBuilder.setTitle("Search Place");

        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        //Call method for handling search for address queried.
                        MapsActivity.this.getLocationFromAddress(input.getText().toString().trim());

                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();

    }


    /**
     * @method startInstalledAppDetailsActivity
     * @desc: launches the permission/setting intent UI for this app to assist for enabling permissions.
     */
    public void startInstalledAppDetailsActivity()
    {
        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(i);
    }


    /**
     *@desc Routing Listener interface methods implemented.
    **/

    @Override
    public void onRoutingFailure(RouteException e) {}

    @Override
    public void onRoutingStart() {}

    @Override
    public void onRoutingSuccess(ArrayList<Route> list, int i)
    {
        //Get all points and plot the polyLine route.
        List<LatLng> listPoints = list.get(0).getPoints();
        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        Iterator<LatLng> iterator = listPoints.iterator();
        while(iterator.hasNext())
        {
            LatLng data = iterator.next();
            options.add(data);
        }

        //If line not null then remove old polyline routing.
        if(line != null)
        {
            line.remove();
        }
        line = mMap.addPolyline(options);

        //Show distance and duration.
        String str = "Distance: " + list.get(0).getDistanceText() + "\nDuration: " + list.get(0).getDurationText();
        Toast.makeText(getApplicationContext(), "" + str, Toast.LENGTH_LONG).show();

        //Focus on map bounds
        mMap.moveCamera(CameraUpdateFactory.newLatLng(list.get(0).getLatLgnBounds().getCenter()));
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(locStart);
        builder.include(locEnd);
        LatLngBounds bounds = builder.build();
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
    }

    @Override
    public void onRoutingCancelled() {}

}

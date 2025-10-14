package com.vijay.jsonwizard.maps;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

import static com.vijay.jsonwizard.maps.MapsUtils.MAX_ZOOM_LEVEL;
import static com.vijay.jsonwizard.maps.MapsUtils.MIN_ZOOM_LEVEL;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.vijay.jsonwizard.R;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_INITIAL_LOCATION = "INITIAL_LOCATION";
    public static final String EXTRA_USE_ACCURACY = "USE_ACCURACY";
    public static final String EXTRA_RESULT_LOCATION = "RESULT_LOCATION";
    public static final String EXTRA_CUSTOM_MARKER_ICON = "CUSTOM_MARKER_ICON";
    public static final String EXTRA_CONFIG_MIN_ZOOM = "CONFIG_MIN_ZOOM";
    public static final String EXTRA_CONFIG_MAX_ZOOM = "CONFIG_MAX_ZOOM";
    public static final String EXTRA_CONFIG_DEFAULT_ZOOM = "CONFIG_DEFAULT_ZOOM";

    private static final String TAG = "JsonFormActivity";
    private static final int REQUEST_CODE_LOCATION = 80;
    private static final int REQUEST_CHECK_SETTINGS = 801;

    private GoogleMap mMap;
    private String mInitialPos;
    private String mMarkerPosition;
    private Marker mMarker;

    private boolean mIncludeAccuracy;
    private String mMarkerIcon;
    private double mMinZoom;
    private double mMaxZoom;
    private double mDefaultZoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        View rootView = findViewById(android.R.id.content);
        Window window = getWindow();

        EdgeToEdge.enable(this);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowCompat.setDecorFitsSystemWindows(window, false);

            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                Insets systemBars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

                v.setPadding(systemBars.left, systemBars.top, systemBars.right, Math.max(systemBars.bottom, ime.bottom));

                return insets;
            });

        } else {
            WindowInsetsControllerCompat controller =
                    new WindowInsetsControllerCompat(window, rootView);
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );

            WindowCompat.setDecorFitsSystemWindows(window, true);
        }

        Toolbar toolbar = findViewById(R.id.tb_top);
        setSupportActionBar(toolbar);
        MapsInitializer.initialize(getApplicationContext(), MapsInitializer.Renderer.LATEST, null);
        getSupportActionBar().setTitle(R.string.choose_a_location);

        // Load extra datas
        Intent intent = getIntent();
        mIncludeAccuracy = intent.getBooleanExtra(EXTRA_USE_ACCURACY, false);

        if (intent.hasExtra(EXTRA_INITIAL_LOCATION)) {
            try {
                mInitialPos = intent.getStringExtra(EXTRA_INITIAL_LOCATION);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Invalid initial position", e);
                attemptMarkCurrentLocation();
            }
        } else {
            // Get current location
            attemptMarkCurrentLocation();
        }

        if (intent.hasExtra(EXTRA_CUSTOM_MARKER_ICON)) {
            mMarkerIcon = intent.getStringExtra(EXTRA_CUSTOM_MARKER_ICON);
        }

        mMinZoom = intent.getDoubleExtra(EXTRA_CONFIG_MIN_ZOOM, MIN_ZOOM_LEVEL);
        if (mMinZoom < 0) {
            mMinZoom = 0;
        }
        mMaxZoom = intent.getDoubleExtra(EXTRA_CONFIG_MAX_ZOOM, MAX_ZOOM_LEVEL);
        if (mMaxZoom < 0) {
            mMaxZoom = 0;
        }
        mDefaultZoom = intent.getDoubleExtra(EXTRA_CONFIG_DEFAULT_ZOOM, MAX_ZOOM_LEVEL);
        if (mDefaultZoom < 0) {
            mMaxZoom = 0;
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                attemptMarkCurrentLocation();
            } else {
                markDefaultPosition();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_location_picker, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_choose) {
            Intent data = new Intent();
            data.putExtra(EXTRA_RESULT_LOCATION, mMarkerPosition);
            setResult(RESULT_OK, data);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION) {
            boolean grantedAny = false;
            for (int grantResult : grantResults) {
                if (grantResult == PERMISSION_GRANTED) {
                    grantedAny = true;
                    break;
                }
            }
            if (grantedAny) {
                attemptMarkCurrentLocation();
            } else {
                Log.w(TAG, "Current location permission not granted");
                markDefaultPosition();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMinZoomPreference((float) mMinZoom);
        mMap.setMaxZoomPreference((float) mMaxZoom);

        if (mInitialPos != null) {
            updateMapMarker(mInitialPos, true);
        }
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                mMarkerPosition = getPositionWithOptionalAccuracy(mMap.getCameraPosition().target, -1);
                updateMapMarker(mMarkerPosition, false);
            }
        });
    }

    private void attemptMarkCurrentLocation() {
        if (!checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) && !checkPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Request Location permission
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);
            } else {
                // Put marker in default position
                markDefaultPosition();
            }
        } else {
            final FusedLocationProviderClient fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(this);

            fusedLocationClient.getLastLocation().addOnCompleteListener(
                    new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            Location location = task.getResult();
                            if (location != null) {
                                updateMarkerLocation(location);
                            } else {
                                Log.w(TAG, "Could not obtain last location", task.getException());
                                initLocationRequest();
                            }
                        }
                    });
        }
    }

    private void markDefaultPosition() {
        String defaultLocation = getResources().getString(R.string.default_location);
        String position;
        if (MapsUtils.isValidPositionString(defaultLocation)) {
            LatLng latLng = MapsUtils.parse(defaultLocation);
            position = getPositionWithOptionalAccuracy(latLng, 0);
        } else {
            position = getPositionWithOptionalAccuracy(new LatLng(0, 0), 0);
        }
        updateMapMarker(position, true);
    }

    private void updateMarkerLocation(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        LatLng latLng = new LatLng(lat, lng);
        float accuracy = location.getAccuracy();

        mInitialPos = getPositionWithOptionalAccuracy(latLng, accuracy);
        updateMapMarker(mInitialPos, true);
    }

    private void initLocationRequest() {
        final LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(MapsActivity.this);
        client.checkLocationSettings(builder.build())
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.e(TAG, "Location settings OK");
                        requestCurrentLocation(locationRequest);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Invalid location settings", e);
                        if (e instanceof ResolvableApiException) {
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                ResolvableApiException resolvable = (ResolvableApiException) e;
                                resolvable.startResolutionForResult(MapsActivity.this, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException sendEx) {
                                // Ignore the error.
                            }
                        }
                    }
                });
    }

    @SuppressLint("MissingPermission")
    private void requestCurrentLocation(final LocationRequest locationRequest) {
        final FusedLocationProviderClient fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(MapsActivity.this);
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                Log.d(TAG, "location available: " + locationAvailability.isLocationAvailable());
            }

            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d(TAG, "Obtained location result");
                if (locationResult != null) {
                    updateMarkerLocation(locationResult.getLastLocation());
                    fusedLocationClient.removeLocationUpdates(this);
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Location request updates started");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Location request updates failed to start", e);
                    }
                });
    }

    private boolean checkPermission(String permission) {
        return ActivityCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void updateMapMarker(String markerPosition, boolean repositionCamera) {
        if (mMap != null) {
            LatLng latLng = MapsUtils.parse(markerPosition);
            if (mMarker == null) {
                mMap.clear();
                MarkerOptions markerOptions = new MarkerOptions()
                        .position(latLng);
                if (mMarkerIcon != null) {
                    markerOptions
                            .icon(BitmapDescriptorFactory.fromPath(mMarkerIcon));
                }
                mMarker = mMap.addMarker(markerOptions);
            } else {
                mMarker.setPosition(latLng);
            }
            if (repositionCamera) {
                CameraPosition pos = CameraPosition.builder().target(latLng).zoom((float) mDefaultZoom)
                        .build();
                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
            }

            mMarkerPosition = markerPosition;
        }
    }

    private String getPositionWithOptionalAccuracy(LatLng latLng, float accuracy) {
        if (mIncludeAccuracy) {
            return MapsUtils.toString(latLng, accuracy);
        } else {
            return MapsUtils.toString(latLng);
        }
    }

}

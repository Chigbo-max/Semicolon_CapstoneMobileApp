
class Location {
  final double latitude;
  final double longitude;

  Location({required this.latitude, required this.longitude});

  Map<String, double> toMap(){
    return{
      'latitude': latitude,
      'longitude': longitude,
  
    };
  }

  @override
  String toString() {
    return 'Location(latitude: $latitude, longitude: $longitude)';
  }
}
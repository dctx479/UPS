import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:flutter_screenutil/flutter_screenutil.dart';
import 'screens/home_screen.dart';
import 'screens/user_list_screen.dart';
import 'screens/profile_screen.dart';
import 'screens/data_visualization_screen.dart';
import 'providers/user_provider.dart';
import 'providers/profile_provider.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => UserProvider()),
        ChangeNotifierProvider(create: (_) => ProfileProvider()),
      ],
      child: ScreenUtilInit(
        designSize: const Size(375, 812),
        minTextAdapt: true,
        splitScreenMode: true,
        builder: (context, child) {
          return MaterialApp(
            title: '用户画像系统',
            debugShowCheckedModeBanner: false,
            theme: ThemeData(
              colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
              useMaterial3: true,
              fontFamily: 'Roboto',
            ),
            initialRoute: '/',
            routes: {
              '/': (context) => const HomeScreen(),
              '/users': (context) => const UserListScreen(),
              '/profile': (context) => const ProfileScreen(),
              '/visualization': (context) => const DataVisualizationScreen(),
            },
          );
        },
      ),
    );
  }
}

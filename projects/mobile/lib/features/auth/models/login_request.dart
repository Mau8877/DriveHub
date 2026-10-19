/// Cuerpo de `POST /auth/login` (CU02). La app siempre inicia como `MOVIL`.
class LoginRequest {
  final String email;
  final String password;

  const LoginRequest({required this.email, required this.password});

  Map<String, dynamic> toJson() => {
    'email': email,
    'password': password,
    'dispositivo': 'MOVIL',
  };
}

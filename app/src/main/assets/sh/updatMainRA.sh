get_recent_apps() {
  # Получаем dumpsys_output, исключая mHiddenTasks=
  dumpsys_output=$(dumpsys activity recents | grep -v 'mHiddenTasks=')

  # Извлекаем recent_apps
  recent_apps=$(echo "$dumpsys_output" | grep -E 'A=|I=' | sed -E 's/.*A=[0-9]+://;s/.*I=//;s/Task\{[0-9a-f]+ //;s/\}//' | sed 's/[^a-zA-Z0-9.-]//g')

  # Возвращаем полученные данные
  echo "$recent_apps"
}

# Преобразование массива в строку с разделителем /=\
convert_to_string() {
  local apps_array=("$@")
  local app_string=""

  for app in "${apps_array[@]}"; do
    app_string+="$app/=/"
  done

  # Удаляем последний разделитель /=\
  echo "${app_string%/=/}"
}

# Инициализация
previous_apps=$(get_recent_apps)

# Основной цикл
while true; do
  # Ждём 0.1 секунды
  sleep 0.1

  # Получаем обновлённые данные recent_apps
  current_apps=$(get_recent_apps)

  # Сравнение с предыдущими данными
  if [[ "$previous_apps" != "$current_apps" ]]; then
    # Находим invalid_apps
    invalid_apps=$(echo "$current_apps" | grep -vE '^[a-zA-Z0-9]+\.[a-zA-Z0-9\.]+$')

    # Если есть invalid_apps, выполняем замену
    if [ -n "$invalid_apps" ]; then
      replacement=$(su -c "dumpsys package | grep -B 20 \"/$invalid_apps\" | grep -oE '[^ ]+/$invalid_apps' | cut -d '/' -f 1 | sort | uniq")
      recent_app_cl=$(echo "$current_apps" | sed "s|$invalid_apps|$replacement|g")
    else
      recent_app_cl="$current_apps"
    fi

    # Преобразуем recent_app_cl в строку
    app_string=$(convert_to_string "${recent_app_cl[@]}")

    # Выполняем команду am broadcast
    am broadcast -a android.intent.action.SEND --es run_function "$app_string" -n com.ppnapptest.имя_вашего_приложения/.IntIntReceiver

    # Обновляем previous_apps
    previous_apps="$current_apps"
  fi
done

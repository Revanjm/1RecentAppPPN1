sh
#!/bin/bash

DB_PATH="/storage/emulated/0/.recentappppn1/.db/main.db"

# Проверяем, существует ли база данных
if [ ! -f "$DB_PATH" ]; then
    exit 1
fi

# Начало измерения времени
start_time=$(date +%s%3N)

# Получаем данные из dumpsys activity, исключая hidden tasks и фильтруя ненужные приложения
recent_apps=$(dumpsys activity | awk '/Recent tasks/{f=1} /mHiddenTasks/{f=0} f && /A=|I=/ {gsub(/.*A=[0-9]+:|.*I=|Task\\{[0-9a-f]+ |\\}/,""); if ($1 !~ /com\.miui\.home|com\.ppnapptest\.quickpanel1/) print $1}')

# Отбор неправильных форматов приложений и получение правильных пакетов
invalid_apps=$(echo "$recent_apps" | grep -vE '^[a-zA-Z0-9]+\.[a-zA-Z0-9\.]+$')
if [ -n "$invalid_apps" ]; then
  replacement=$(su -c "dumpsys package | grep -B 20 \"/$invalid_apps\" | grep -oE '[^ ]+/$invalid_apps' | cut -d '/' -f 1 | sort | uniq")
  recent_apps=$(echo "$recent_apps" | sed "s|$invalid_apps|$replacement|g")
fi

# Преобразуем список приложений в массив
apps_array=($(echo "$recent_apps" | tr '\n' ' '))

columns="id"
values="1"

# Создаем список колонок и значений для записи в базу данных
for i in "${!apps_array[@]}"; do
    column_name="ra$(($i + 1))"
    columns+=", $column_name"
    value="${apps_array[$i]}"
    values+=", \"$value\""
done

# Выполняем SQLite команду с параметром -nocopy
sqlite3 -nocopy "$DB_PATH" <<EOF
PRAGMA journal_mode = MEMORY;
BEGIN TRANSACTION;
DELETE FROM main WHERE id = 1;
INSERT INTO main ($columns)
VALUES ($values);
COMMIT;
EOF

# Окончание измерения времени
end_time=$(date +%s%3N)
execution_time=$((end_time - start_time))

# Финальный вывод после завершения скрипта
echo "Время выполнения скрипта: $execution_time мс"
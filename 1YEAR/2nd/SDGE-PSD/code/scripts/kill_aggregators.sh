for i in test/tmp/*.pid; do
    cat $i | xargs kill -9
done
rm test/tmp/*.pid IPSet.txt
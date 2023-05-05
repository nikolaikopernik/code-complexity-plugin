@complexity(1)
def generate_data(df: pd.DataFrame):
    """
    :param df: dataframe includes only positive examples
    :return: dataframe that have both positive and negative examples included
    """
    for number_negative in number_negative_list:                            # +1
        df = df.copy()
        df.loc[:, 'text'] = df_1.text.map(lambda x: x[0:number_negative])
        df.loc[:, 'ids'] = df_1.ids.map(lambda x: x[0:number_negative])
        df.loc[:, 'scores'] = df_1.scores.map(lambda x: x[0:number_negative])
    return df_combine_list
